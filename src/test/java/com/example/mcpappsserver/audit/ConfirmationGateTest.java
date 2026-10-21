package com.example.mcpappsserver.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationGateTest {

  @TempDir
  Path tmp;

  @Test
  void proposeThenConfirmRunsOnce() {
    ConfirmationGate gate = new ConfirmationGate();
    AtomicBoolean ran = new AtomicBoolean(false);
    PendingAction pending = gate.propose("demo-write", "k=v", () -> {
      ran.set(true);
      return "ok";
    });
    assertEquals(1, gate.pendingCount());
    assertEquals("ok", gate.confirm(pending.confirmToken()).orElseThrow());
    assertTrue(ran.get());
    assertEquals(0, gate.pendingCount());
    assertTrue(gate.confirm(pending.confirmToken()).isEmpty());
  }

  @Test
  void badTokenDenied() {
    ConfirmationGate gate = new ConfirmationGate();
    assertTrue(gate.confirm("cfm-nope").isEmpty());
  }

  @Test
  void auditAppendAndRecent() throws Exception {
    var ctor = AuditLogStore.class.getDeclaredConstructor(Path.class);
    ctor.setAccessible(true);
    AuditLogStore store = ctor.newInstance(tmp.resolve("audit.jsonl"));
    store.append("agent", "list-account-notes", "", "ALLOWED_READ", "ok");
    store.append("agent", "propose-demo-transfer", "amt=1", "PENDING", "tok");
    assertEquals(2, store.size());
    assertEquals("PENDING", store.recent(1).get(0).decision());
    assertFalse(store.recent(10).isEmpty());
  }
}
