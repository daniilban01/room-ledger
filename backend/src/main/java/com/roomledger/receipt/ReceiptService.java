package com.roomledger.receipt;

import com.roomledger.common.error.BadRequestException;
import com.roomledger.common.error.TooManyRequestsException;
import com.roomledger.group.GroupAccess;
import com.roomledger.receipt.dto.ReceiptDraftResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Deliberately NOT @Transactional: reading a receipt calls an external service that can take many
 * seconds, and holding a database connection open for that long would starve the connection pool.
 */
@Service
public class ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    private final ReceiptParser parser;
    private final ReceiptRateLimiter rateLimiter;
    private final GroupAccess groupAccess;
    private final ReceiptValidator validator = new ReceiptValidator();

    public ReceiptService(ReceiptParser parser, ReceiptRateLimiter rateLimiter, GroupAccess groupAccess) {
        this.parser = parser;
        this.rateLimiter = rateLimiter;
        this.groupAccess = groupAccess;
    }

    public ReceiptDraftResponse parse(UUID groupId, UUID userId, MultipartFile image) {
        groupAccess.requireMember(groupId, userId);

        byte[] bytes = readBytes(image);
        ImageType type = ImageType.detect(bytes)
                .orElseThrow(() -> new BadRequestException("Only JPEG, PNG or WebP images are accepted"));

        // Checked after validating the file, so a wrong file type does not use up an attempt.
        rateLimiter.tryAcquire(userId).ifPresent(retryAfter -> {
            throw new TooManyRequestsException("Too many receipts scanned in the last hour", retryAfter);
        });

        try {
            return validator.validate(parser.parse(bytes, type.mediaType));
        } catch (UnreadableReceiptException e) {
            log.info("Receipt could not be read: {}", e.getMessage());
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "The receipt could not be read. Try a sharper photo.");
        } catch (ReceiptProviderException e) {
            // The client gets a generic message, but we must log the real cause (HTTP status and the
            // provider's error body: wrong model, quota exceeded, invalid key...) or we cannot debug it.
            log.warn("Receipt provider call failed", e);
            throw error(HttpStatus.BAD_GATEWAY, "The receipt reading service is not available right now.");
        }
    }

    private static byte[] readBytes(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("An image is required");
        }
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("The image could not be read");
        }
    }

    private static ErrorResponseException error(HttpStatus status, String detail) {
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
