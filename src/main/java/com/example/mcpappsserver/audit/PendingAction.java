package com.example.mcpappsserver.audit;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * A mutating action waiting for an explicit confirm token.
 */
public record PendingAction(
    String confirmToken,
    String toolName,
    String argsSummary,
    Instant createdAt,
    Instant expiresAt,
    Supplier<String> action) {
}
