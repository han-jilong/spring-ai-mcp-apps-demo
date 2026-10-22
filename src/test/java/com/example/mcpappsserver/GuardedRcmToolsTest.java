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
 * RCM vertical flow over GuardedRcmTools + shared gate/confirm from GuardedAccountTools.
 */
class GuardedRcmToolsTest {

  @TempDir
  Path tmp;

  @Test
  void readPendingApprovedHardDenyAndJsonl() throws Exception {
    Path logFile = tmp.resolve("rcm-audit-log.jsonl");
    AuditLogStore store = newStore(logFile);
    ConfirmationGate gate = new ConfirmationGate();
    GuardedRcmTools rcm = new GuardedRcmTools(store, gate);
    GuardedAccountTools account = new GuardedAccountTools(store, gate);

    String listed = rcm.listSuggestedCharges("CLAIM-DEMO-001");
    assertTrue(listed.contains("DEMO-CHG-100"), listed);
    assertTrue(listed.contains("DEMO-PATIENT"), listed);
    assertTrue(listed.contains("fictional"), listed);

    String draft = rcm.getClaimDraft("CLAIM-DEMO-001");
    assertTrue(draft.contains("X99.1"), draft);
    assertTrue(draft.contains("DEMO-PATIENT"), draft);

    assertTrue(store.recent(10).stream().anyMatch(e ->
        "ALLOWED_READ".equals(e.decision()) && "list-suggested-charges".equals(e.toolName())));
    assertTrue(store.recent(10).stream().anyMatch(e ->
        "ALLOWED_READ".equals(e.decision()) && "get-claim-draft".equals(e.toolName())));

    String hard = rcm.proposeApplyChargeCodes("CLAIM-DEMO-001", "DEMO-CHG-999", "10000.01");
    assertTrue(hard.startsWith("DENIED:"), hard);
    assertTrue(hard.contains("hard limit"), hard);

    String hardCount = rcm.proposeApplyChargeCodes(
        "CLAIM-DEMO-001",
        "C1,C2,C3,C4,C5,C6,C7,C8,C9,C10,C11,C12,C13,C14,C15,C16,C17,C18,C19,C20,C21",
        "100");
    assertTrue(hardCount.startsWith("DENIED:"), hardCount);
    assertTrue(hardCount.contains("code count"), hardCount);

    String soft = rcm.proposeApplyChargeCodes(
        "CLAIM-DEMO-001", "DEMO-CHG-100,X99.1", "600");
    assertTrue(soft.startsWith("PENDING"), soft);
    assertTrue(soft.contains("soft threshold"), soft);
    assertTrue(soft.contains("confirmToken="), soft);
    String token = soft.replaceAll("(?s).*confirmToken=([a-z0-9-]+).*", "$1");

    String applied = account.confirmPendingAction(token);
    assertTrue(applied.startsWith("APPLIED:"), applied);
    assertTrue(applied.contains("DEMO-CHG-100"), applied);
    assertEquals(1, rcm.appliedLedgerSnapshot().size());

    List<AuditEntry> rows = store.recent(30);
    assertTrue(rows.stream().anyMatch(e -> "DENIED".equals(e.decision())));
    assertTrue(rows.stream().anyMatch(e -> "PENDING".equals(e.decision())
        && "propose-apply-charge-codes".equals(e.toolName())));
    assertTrue(rows.stream().anyMatch(e -> "APPROVED".equals(e.decision())));

    assertTrue(Files.isRegularFile(logFile));
    assertTrue(Files.size(logFile) > 0);
    String jsonl = Files.readString(logFile);
    assertTrue(jsonl.contains("propose-apply-charge-codes") || jsonl.contains("ALLOWED_READ"),
        "JSONL should contain at least one RCM/audit path");

    AuditLogStore reloaded = newStore(logFile);
    assertTrue(reloaded.size() >= 4, "expected read+deny+pending+approved rows reloaded");
  }

  private static AuditLogStore newStore(Path file) throws Exception {
    var ctor = AuditLogStore.class.getDeclaredConstructor(Path.class);
    ctor.setAccessible(true);
    return ctor.newInstance(file);
  }
}
