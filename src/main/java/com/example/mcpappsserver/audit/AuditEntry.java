package com.example.mcpappsserver.audit;

import java.time.Instant;

/**
 * One append-only audit row: who / tool / args summary / decision / when.
 */
public record AuditEntry(
    String id,
    Instant timestamp,
    String actor,
    String toolName,
    String argsSummary,
    String decision,
    String detail) {
}
