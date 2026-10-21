package com.example.mcpappsserver;

import com.example.mcpappsserver.audit.AuditEntry;
import com.example.mcpappsserver.audit.AuditLogStore;
import com.example.mcpappsserver.audit.ConfirmationGate;
import com.example.mcpappsserver.audit.PendingAction;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Minimal Agent audit-guardrail demo tools.
 * <ul>
 *   <li>Read-only tools run immediately (no confirm gate).</li>
 *   <li>Mutating tools require an explicit confirm token.</li>
 *   <li>Soft threshold (100 CNY) cues PENDING; hard limit (10000 CNY) auto-denies.</li>
 *   <li>Audit rows persist to JSONL so restart keeps history.</li>
 * </ul>
 * Does not touch {@link DiceApp} — MCP Apps UI demo stays intact.
 */
@Service
public class GuardedAccountTools {

  /** Soft threshold (CNY): narrative cue in PENDING message when exceeded. */
  public static final BigDecimal CONFIRM_THRESHOLD = new BigDecimal("100");
  /** Hard deny (CNY): refused even with confirm. */
  public static final BigDecimal HARD_LIMIT = new BigDecimal("10000");

  private final AuditLogStore auditLog;
  private final ConfirmationGate gate;
  private final Map<String, String> notes = new ConcurrentHashMap<>();
  private BigDecimal demoBalance = new BigDecimal("5000.00");

  public GuardedAccountTools(AuditLogStore auditLog, ConfirmationGate gate) {
    this.auditLog = auditLog;
    this.gate = gate;
    notes.put("welcome", "demo account note — read freely");
  }

  // ---- Read-only (no gate) ----

  @McpTool(
      name = "list-account-notes",
      title = "List account notes (read-only)",
      description = "Read-only: list demo account notes. No confirmation required.")
  public String listAccountNotes() {
    String summary = notes.isEmpty() ? "(empty)" : notes.entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("; "));
    auditLog.append("agent", "list-account-notes", "", "ALLOWED_READ", summary);
    return "notes: " + summary;
  }

  @McpTool(
      name = "get-demo-balance",
      title = "Get demo balance (read-only)",
      description = "Read-only: return the in-memory demo balance. No confirmation required.")
  public String getDemoBalance() {
    auditLog.append("agent", "get-demo-balance", "", "ALLOWED_READ", "balance=" + demoBalance);
    return "demoBalanceCNY=" + demoBalance.toPlainString();
  }

  @McpTool(
      name = "get-audit-log",
      title = "Get audit log (read-only)",
      description = "Read-only: recent audit rows (who/tool/args/decision/time) from in-memory ring (max 500) backed by JSONL file (survives restart). No confirmation required.")
  public String getAuditLog(
      @McpToolParam(description = "Max rows (1-50)", required = false) Integer limit) {
    int n = limit == null ? 20 : limit;
    List<AuditEntry> rows = auditLog.recent(n);
    String body;
    if (rows.isEmpty()) {
      body = "audit: (empty)";
    }
    else {
      StringBuilder sb = new StringBuilder("audit (").append(rows.size()).append("):\n");
      for (AuditEntry e : rows) {
        sb.append("- [").append(e.id()).append("] ")
            .append(e.timestamp()).append(" | ")
            .append(e.decision()).append(" | ")
            .append(e.toolName()).append(" | args=")
            .append(e.argsSummary()).append(" | ")
            .append(e.detail()).append("\n");
      }
      body = sb.toString();
    }
    auditLog.append("agent", "get-audit-log", "limit=" + n, "ALLOWED_READ", "returned=" + rows.size());
    return body;
  }

  // ---- Mutating (confirmation gate) ----

  @McpTool(
      name = "propose-set-account-note",
      title = "Propose set account note (needs confirm)",
      description = "Mutating: propose writing a note. Does NOT write until confirm-pending-action with the returned confirmToken.")
  public String proposeSetAccountNote(
      @McpToolParam(description = "Note key", required = true) String key,
      @McpToolParam(description = "Note value", required = true) String value) {
    if (key == null || key.isBlank() || value == null) {
      auditLog.append("agent", "propose-set-account-note", "key/value missing", "DENIED", "invalid args");
      return "DENIED: key and value are required";
    }
    String safeKey = key.trim();
    String safeValue = value.length() > 200 ? value.substring(0, 200) : value;
    String args = "key=" + safeKey + ",valueLen=" + safeValue.length();

    PendingAction pending = gate.propose("set-account-note", args, () -> {
      notes.put(safeKey, safeValue);
      return "APPLIED: note '" + safeKey + "' set";
    });

    auditLog.append("agent", "propose-set-account-note", args, "PENDING",
        "confirmToken=" + pending.confirmToken() + " expires=" + pending.expiresAt());

    return "PENDING confirmation. Call confirm-pending-action with confirmToken="
        + pending.confirmToken()
        + " (expires " + pending.expiresAt() + "). Preview: set note '"
        + safeKey + "' → '" + safeValue + "'";
  }

  @McpTool(
      name = "propose-demo-transfer",
      title = "Propose demo transfer (needs confirm / amount limit)",
      description = "Mutating: propose a demo CNY transfer. Soft threshold CONFIRM_THRESHOLD=100 CNY → PENDING message flags over-soft; hard limit HARD_LIMIT=10000 CNY → DENIED immediately (even with a token). All amounts still need confirm-pending-action before balance changes.")
  public String proposeDemoTransfer(
      @McpToolParam(description = "Payee label", required = true) String to,
      @McpToolParam(description = "Amount in CNY", required = true) String amountYuan) {
    String args = "to=" + to + ",amountYuan=" + amountYuan;
    BigDecimal amount;
    try {
      amount = new BigDecimal(amountYuan.trim());
    }
    catch (Exception ex) {
      auditLog.append("agent", "propose-demo-transfer", args, "DENIED", "bad amount");
      return "DENIED: amountYuan must be a number";
    }
    if (amount.signum() <= 0) {
      auditLog.append("agent", "propose-demo-transfer", args, "DENIED", "non-positive");
      return "DENIED: amount must be positive";
    }
    if (amount.compareTo(HARD_LIMIT) > 0) {
      auditLog.append("agent", "propose-demo-transfer", args, "DENIED",
          "exceeds hard limit " + HARD_LIMIT);
      return "DENIED: amount exceeds hard limit CNY " + HARD_LIMIT.toPlainString()
          + " (amount-limit guardrail)";
    }
    if (amount.compareTo(demoBalance) > 0) {
      auditLog.append("agent", "propose-demo-transfer", args, "DENIED", "insufficient balance");
      return "DENIED: insufficient demo balance " + demoBalance.toPlainString();
    }

    final BigDecimal amt = amount;
    final String payee = to == null ? "" : to.trim();
    boolean overSoft = amt.compareTo(CONFIRM_THRESHOLD) > 0;

    PendingAction pending = gate.propose("demo-transfer", args, () -> {
      demoBalance = demoBalance.subtract(amt);
      return "APPLIED: transferred CNY " + amt.toPlainString() + " to '" + payee
          + "'; remaining=" + demoBalance.toPlainString();
    });

    auditLog.append("agent", "propose-demo-transfer", args, "PENDING",
        "confirmToken=" + pending.confirmToken()
            + (overSoft ? " overSoftThreshold" : "")
            + " expires=" + pending.expiresAt());

    return "PENDING confirmation"
        + (overSoft ? " (amount > soft threshold CNY " + CONFIRM_THRESHOLD + ")" : "")
        + ". Call confirm-pending-action with confirmToken="
        + pending.confirmToken()
        + ". Preview: pay '" + payee + "' CNY " + amt.toPlainString()
        + " from balance " + demoBalance.toPlainString();
  }

  @McpTool(
      name = "confirm-pending-action",
      title = "Confirm pending mutating action",
      description = "Consumes a confirmToken from propose-* tools and executes the pending write. Invalid/expired tokens are DENIED and audited.")
  public String confirmPendingAction(
      @McpToolParam(description = "Token from a propose-* tool", required = true) String confirmToken) {
    String token = confirmToken == null ? "" : confirmToken.trim();
    String args = "confirmToken=" + token;
    Optional<PendingAction> peek = gate.peek(token);
    Optional<String> result = gate.confirm(token);

    if (result.isEmpty()) {
      auditLog.append("agent", "confirm-pending-action", args, "DENIED",
          "invalid or expired token");
      return "DENIED: invalid or expired confirmToken";
    }

    String tool = peek.map(PendingAction::toolName).orElse("unknown");
    auditLog.append("agent", "confirm-pending-action", args, "APPROVED",
        "tool=" + tool + " → " + result.get());
    return result.get();
  }
}
