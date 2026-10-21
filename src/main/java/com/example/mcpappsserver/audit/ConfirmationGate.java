package com.example.mcpappsserver.audit;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Explicit confirmation gate for write/mutating tools.
 * Read-only tools never call this.
 */
@Component
public class ConfirmationGate {

  private static final Duration TTL = Duration.ofMinutes(10);

  private final Map<String, PendingAction> pending = new ConcurrentHashMap<>();

  public PendingAction propose(String toolName, String argsSummary, Supplier<String> action) {
    purgeExpired();
    String token = "cfm-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Instant now = Instant.now();
    PendingAction pa = new PendingAction(token, toolName, argsSummary, now, now.plus(TTL), action);
    pending.put(token, pa);
    return pa;
  }

  public Optional<String> confirm(String confirmToken) {
    purgeExpired();
    if (confirmToken == null || confirmToken.isBlank()) {
      return Optional.empty();
    }
    PendingAction pa = pending.remove(confirmToken.trim());
    if (pa == null) {
      return Optional.empty();
    }
    if (Instant.now().isAfter(pa.expiresAt())) {
      return Optional.empty();
    }
    return Optional.ofNullable(pa.action().get());
  }

  public Optional<PendingAction> peek(String confirmToken) {
    purgeExpired();
    return Optional.ofNullable(pending.get(confirmToken));
  }

  public int pendingCount() {
    purgeExpired();
    return pending.size();
  }

  private void purgeExpired() {
    Instant now = Instant.now();
    pending.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
  }
}
