package com.example.mcpappsserver;

import com.example.mcpappsserver.audit.AuditLogStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * ChatClient / {@code @Tool} path aligned with Spring AI 2.1+ official
 * <a href="https://docs.spring.io/spring-ai/reference/2.1/api/tools/tool-call-confirmation.html">Tool Call Confirmation</a>.
 * <p>
 * Mutating tools set {@code requiresConfirmation = true} so {@code ToolCallingManager} pauses
 * until the app supplies {@code ToolCallDecision.allow}/{@code deny} and resumes via
 * {@code executeToolCalls(prompt, reply, decisions)}. Read-only tools stay ungated.
 * <p>
 * This is the <strong>ChatClient</strong> path. MCP clients keep using
 * {@link GuardedAccountTools} + {@link com.example.mcpappsserver.audit.ConfirmationGate}
 * (explicit {@code confirmToken}); both share the same sandbox ledger / audit store.
 * No live Stripe / Alipay — demo balance only.
 */
@Service
public class TreasuryChatTools {

  private final GuardedAccountTools accountTools;
  private final AuditLogStore auditLog;

  public TreasuryChatTools(GuardedAccountTools accountTools, AuditLogStore auditLog) {
    this.accountTools = accountTools;
    this.auditLog = auditLog;
  }

  // ---- Read-only (no requiresConfirmation) ----

  @Tool(description = "Read-only: list demo account notes. No confirmation required.")
  public String listAccountNotes() {
    return accountTools.listAccountNotes();
  }

  @Tool(description = "Read-only: return the in-memory demo balance (sandbox CNY). No confirmation required.")
  public String getDemoBalance() {
    return accountTools.getDemoBalance();
  }

  @Tool(description = "Read-only: recent audit rows from JSONL-backed store. No confirmation required.")
  public String getAuditLog(
      @ToolParam(description = "Max rows (1-50)", required = false) Integer limit) {
    return accountTools.getAuditLog(limit);
  }

  // ---- Mutating (official requiresConfirmation) ----

  @Tool(
      description = "Mutating: set a demo account note on the sandbox ledger. "
          + "Marked requiresConfirmation — ChatClient/ToolCallingManager pauses until ToolCallDecisions.allow/deny.",
      requiresConfirmation = true)
  public String setAccountNote(
      @ToolParam(description = "Note key", required = true) String key,
      @ToolParam(description = "Note value", required = true) String value) {
    String result = accountTools.applySetAccountNote(key, value);
    String decision = result.startsWith("DENIED") ? "DENIED" : "APPROVED";
    auditLog.append("chat-tool", "setAccountNote",
        "key=" + (key == null ? "" : key.trim()), decision,
        "via official requiresConfirmation → " + result);
    return result;
  }

  @Tool(
      description = "Mutating: sandbox CNY transfer (demo ledger only, no real money). "
          + "Hard limit HARD_LIMIT CNY still DENIED inside the tool. "
          + "Marked requiresConfirmation — pauses for ToolCallDecisions before execution.",
      requiresConfirmation = true)
  public String demoTransfer(
      @ToolParam(description = "Payee label", required = true) String to,
      @ToolParam(description = "Amount in CNY", required = true) String amountYuan) {
    String args = "to=" + to + ",amountYuan=" + amountYuan;
    BigDecimal amount;
    try {
      amount = new BigDecimal(amountYuan == null ? "" : amountYuan.trim());
    }
    catch (Exception ex) {
      auditLog.append("chat-tool", "demoTransfer", args, "DENIED", "bad amount");
      return "DENIED: amountYuan must be a number";
    }
    String result = accountTools.applyDemoTransfer(to, amount);
    String decision = result.startsWith("DENIED") ? "DENIED" : "APPROVED";
    auditLog.append("chat-tool", "demoTransfer", args, decision,
        "via official requiresConfirmation → " + result);
    return result;
  }
}
