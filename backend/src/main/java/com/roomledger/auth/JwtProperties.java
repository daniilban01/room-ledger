package com.roomledger.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Bound from app.jwt.* in application.yml. Validated at startup, so a missing or
 * too-short secret stops the app immediately instead of failing on the first login.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        // HS256 requires a key of at least 256 bits = 32 bytes.
        @NotNull @Size(min = 32) String secret,
        @NotNull Duration expiration) {
}
