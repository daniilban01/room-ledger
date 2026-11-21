package com.roomledger.receipt;

import com.roomledger.auth.AuthenticatedUser;
import com.roomledger.receipt.dto.ReceiptDraftResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /** Multipart upload of one image; returns a draft. Nothing is saved, not even the image. */
    @PostMapping(path = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptDraftResponse parse(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable UUID groupId,
                                      @RequestPart("image") MultipartFile image) {
        return receiptService.parse(groupId, currentUser.id(), image);
    }
}
