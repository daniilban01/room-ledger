package com.roomledger.receipt;

import java.util.Optional;

/**
 * Recognizes an image by its first bytes ("magic numbers"). The file name and the Content-Type sent
 * by the client are just claims anyone can fake; the bytes are what the file actually is.
 */
enum ImageType {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    final String mediaType;

    ImageType(String mediaType) {
        this.mediaType = mediaType;
    }

    static Optional<ImageType> detect(byte[] bytes) {
        if (startsWith(bytes, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (startsWith(bytes, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        // WebP: "RIFF", 4 bytes of file size, then "WEBP".
        if (startsWith(bytes, 0, 'R', 'I', 'F', 'F') && startsWith(bytes, 8, 'W', 'E', 'B', 'P')) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] bytes, int offset, int... expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
