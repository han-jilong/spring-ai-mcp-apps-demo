package com.example.mcpappsserver;

import com.example.mcpappsserver.audit.AuditEntry;
import com.example.mcpappsserver.audit.AuditLogStore;
import com.example.mcpappsserver.audit.ConfirmationGate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style flow over GuardedAccountTools (no Spring context / no MCP wire).
 */
class GuardedAccountToolsTest {

  @TempDir
  Path tmp;

  @Test
  void hardLimitDeniedSoftThresholdFlaggedAndAuditSurvivesRestart() throws Exception {
    Path logFile = tmp.resolve("audit-log.jsonl");
    AuditLogStore store = newStore(logFile);
    ConfirmationGate gate = new ConfirmationGate();
    GuardedAccountTools tools = new GuardedAccountTools(store, gate);

    String hard = tools.proposeDemoTransfer("alice", "10000.01");
    assertTrue(hard.startsWith("DENIED:"), hard);
    assertTrue(hard.contains("hard limit"), hard);

    String soft = tools.proposeDemoTransfer("bob", "150");
    assertTrue(soft.startsWith("PENDING"), soft);
    assertTrue(soft.contains("soft threshold"), soft);
    assertTrue(soft.contains("confirmToken="), soft);
    String token = soft.replaceAll("(?s).*confirmToken=([a-z0-9-]+).*", "$1");
    String applied = tools.confirmPendingAction(token);
    assertTrue(applied.startsWith("APPLIED:"), applied);

    List<AuditEntry> rows = store.recent(20);
    assertTrue(rows.stream().anyMatch(e -> "DENIED".equals(e.decision())));
    assertTrue(rows.stream().anyMatch(e -> "APPROVED".equals(e.decision())));
    assertTrue(Files.isRegularFile(logFile));
    assertTrue(Files.size(logFile) > 0);

    // Restart simulation: new store from same file keeps history
    AuditLogStore reloaded = newStore(logFile);
    assertTrue(reloaded.size() >= 3, "expected DENIED+PENDING+APPROVED rows reloaded");
    assertEquals("APPROVED", reloaded.recent(1).get(0).decision());
  }

  private static AuditLogStore newStore(Path file) throws Exception {
    var ctor = AuditLogStore.class.getDeclaredConstructor(Path.class);
    ctor.setAccessible(true);
    return ctor.newInstance(file);
  }
}
