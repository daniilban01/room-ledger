package com.roomledger.auth;

import java.util.UUID;

/**
 * The principal stored in the SecurityContext for an authenticated request.
 * Controllers receive it with {@code @AuthenticationPrincipal}.
 */
public record AuthenticatedUser(UUID id, String email) {
}
