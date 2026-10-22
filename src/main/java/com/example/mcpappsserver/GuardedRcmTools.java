package com.example.mcpappsserver;

import com.example.mcpappsserver.audit.AuditLogStore;
import com.example.mcpappsserver.audit.ConfirmationGate;
import com.example.mcpappsserver.audit.PendingAction;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Healthcare RCM vertical demo on top of the shared audit gate.
 * <ul>
 *   <li>All charge/diagnosis codes and patients are fictional (no PHI / no live clearinghouse).</li>
 *   <li>Read tools return immediately with {@code ALLOWED_READ}.</li>
 *   <li>{@code propose-apply-charge-codes} only returns PENDING + confirmToken;
 *       apply via shared {@code confirm-pending-action} on {@link GuardedAccountTools}.</li>
 *   <li>Soft amount threshold cues PENDING; hard amount / code-count limits DENY.</li>
 * </ul>
 * Does not touch {@link DiceApp}. Not clinical advice.
 */
@Service
public class GuardedRcmTools {

  /** Soft amount (CNY): PENDING message flags over-soft (still needs confirm). */
  public static final BigDecimal SOFT_AMOUNT_THRESHOLD = new BigDecimal("500");
  /** Hard amount (CNY): refused even with confirm. */
  public static final BigDecimal HARD_AMOUNT_LIMIT = new BigDecimal("10000");
  /** Soft code count: PENDING message flags many codes. */
  public static final int SOFT_CODE_COUNT = 5;
  /** Hard code count: DENIED immediately. */
  public static final int HARD_CODE_COUNT = 20;

  private static final String DEMO_PATIENT = "DEMO-PATIENT";

  private final AuditLogStore auditLog;
  private final ConfirmationGate gate;
  /** Applied claim lines after confirm (in-memory demo ledger). */
  private final List<String> appliedLedger = new CopyOnWriteArrayList<>();

  public GuardedRcmTools(AuditLogStore auditLog, ConfirmationGate gate) {
    this.auditLog = auditLog;
    this.gate = gate;
  }

  // ---- Read-only (no gate) ----

  @McpTool(
      name = "list-suggested-charges",
      title = "List suggested charge codes (read-only, fictional)",
      description = "Read-only: fictional AI-suggested charge/diagnosis codes for DEMO-PATIENT. No PHI. No confirmation required.")
  public String listSuggestedCharges(
      @McpToolParam(description = "Claim draft id (demo)", required = false) String claimId) {
    String id = (claimId == null || claimId.isBlank()) ? "CLAIM-DEMO-001" : claimId.trim();
    List<Map<String, String>> suggestions = demoSuggestions();
    String summary = suggestions.stream()
        .map(m -> m.get("code") + "(" + m.get("type") + ",CNY" + m.get("amountYuan") + ")")
        .collect(Collectors.joining("; "));
    auditLog.append("agent", "list-suggested-charges", "claimId=" + id + ",patient=" + DEMO_PATIENT,
        "ALLOWED_READ", "count=" + suggestions.size() + " " + summary);
    return "claimId=" + id + " patient=" + DEMO_PATIENT
        + " (fictional, not PHI) suggestions: " + summary
        + " | disclaimer: demo only — not clinical advice, no live claims";
  }

  @McpTool(
      name = "get-claim-draft",
      title = "Get claim draft (read-only, fictional)",
      description = "Read-only: fictional claim draft for DEMO-PATIENT with sample codes. No PHI. No confirmation required.")
  public String getClaimDraft(
      @McpToolParam(description = "Claim draft id (demo)", required = false) String claimId) {
    String id = (claimId == null || claimId.isBlank()) ? "CLAIM-DEMO-001" : claimId.trim();
    String body = "claimDraft{id=" + id
        + ", patient=" + DEMO_PATIENT
        + ", status=DRAFT"
        + ", diagnosis=[X99.1, Z00-DEMO]"
        + ", charges=[DEMO-CHG-100@120, DEMO-CHG-200@80]"
        + ", totalYuan=200"
        + ", appliedLedgerSize=" + appliedLedger.size()
        + "} fictional-only";
    auditLog.append("agent", "get-claim-draft", "claimId=" + id + ",patient=" + DEMO_PATIENT,
        "ALLOWED_READ", body);
    return body;
  }

  // ---- Mutating (confirmation gate; confirm via GuardedAccountTools.confirm-pending-action) ----

  @McpTool(
      name = "propose-apply-charge-codes",
      title = "Propose apply charge codes (needs confirm)",
      description = "Mutating: propose applying fictional charge/diagnosis codes to a demo claim. Soft amount "
          + "threshold SOFT_AMOUNT_THRESHOLD CNY / SOFT_CODE_COUNT codes → PENDING cue; hard "
          + "HARD_AMOUNT_LIMIT CNY or HARD_CODE_COUNT codes → DENIED. Does NOT apply until "
          + "confirm-pending-action with the returned confirmToken. Demo only — no PHI / no live clearinghouse.")
  public String proposeApplyChargeCodes(
      @McpToolParam(description = "Claim draft id", required = true) String claimId,
      @McpToolParam(description = "Comma-separated fictional codes, e.g. DEMO-CHG-100,X99.1", required = true)
      String codes,
      @McpToolParam(description = "Total amount in CNY for this apply batch", required = true)
      String amountYuan) {
    String id = claimId == null ? "" : claimId.trim();
    String rawCodes = codes == null ? "" : codes.trim();
    String args = "claimId=" + id + ",codes=" + rawCodes + ",amountYuan=" + amountYuan
        + ",patient=" + DEMO_PATIENT;

    if (id.isBlank() || rawCodes.isBlank()) {
      auditLog.append("agent", "propose-apply-charge-codes", args, "DENIED", "missing claimId/codes");
      return "DENIED: claimId and codes are required (demo fictional codes only)";
    }

    List<String> codeList = splitCodes(rawCodes);
    if (codeList.isEmpty()) {
      auditLog.append("agent", "propose-apply-charge-codes", args, "DENIED", "empty codes");
      return "DENIED: no usable codes after split";
    }
    if (codeList.size() > HARD_CODE_COUNT) {
      auditLog.append("agent", "propose-apply-charge-codes", args, "DENIED",
          "exceeds hard code count " + HARD_CODE_COUNT);
      return "DENIED: code count " + codeList.size() + " exceeds hard limit "
          + HARD_CODE_COUNT + " (RCM code-count guardrail)";
    }

    BigDecimal amount;
    try {
      amount = new BigDecimal(amountYuan.trim());
    }
    catch (Exception ex) {
      auditLog.append("agent", "propose-apply-charge-codes", args, "DENIED", "bad amount");
      return "DENIED: amountYuan must be a number";
    }
    if (amount.signum() <= 0) {
      auditLog.append("agent", "propose-apply-charge-codes", args, "DENIED", "non-positive");
      return "DENIED: amount must be positive";
    }
    if (amount.compareTo(HARD_AMOUNT_LIMIT) > 0) {
      auditLog.append("agent", "propose-apply-charge-codes", args, "DENIED",
          "exceeds hard amount " + HARD_AMOUNT_LIMIT);
      return "DENIED: amount exceeds hard limit CNY " + HARD_AMOUNT_LIMIT.toPlainString()
          + " (RCM amount-limit guardrail)";
    }

    final BigDecimal amt = amount;
    final List<String> lockedCodes = List.copyOf(codeList);
    boolean overSoftAmount = amt.compareTo(SOFT_AMOUNT_THRESHOLD) > 0;
    boolean overSoftCount = lockedCodes.size() > SOFT_CODE_COUNT;

    PendingAction pending = gate.propose("apply-charge-codes", args, () -> {
      String line = "claim=" + id + " patient=" + DEMO_PATIENT
          + " codes=" + String.join(",", lockedCodes)
          + " amountYuan=" + amt.toPlainString();
      appliedLedger.add(line);
      return "APPLIED: " + line + " (demo ledger size=" + appliedLedger.size() + ")";
    });

    String cues = "";
    if (overSoftAmount) {
      cues += " overSoftAmount";
    }
    if (overSoftCount) {
      cues += " overSoftCodeCount";
    }

    auditLog.append("agent", "propose-apply-charge-codes", args, "PENDING",
        "confirmToken=" + pending.confirmToken() + cues + " expires=" + pending.expiresAt());

    return "PENDING confirmation"
        + (overSoftAmount ? " (amount > soft threshold CNY " + SOFT_AMOUNT_THRESHOLD + ")" : "")
        + (overSoftCount ? " (code count > soft " + SOFT_CODE_COUNT + ")" : "")
        + ". Call confirm-pending-action with confirmToken="
        + pending.confirmToken()
        + ". Preview: apply [" + String.join(",", lockedCodes) + "] CNY "
        + amt.toPlainString() + " on claim '" + id + "' for " + DEMO_PATIENT
        + " (fictional — not clinical advice)";
  }

  /** Package-visible for tests. */
  List<String> appliedLedgerSnapshot() {
    return List.copyOf(appliedLedger);
  }

  private static List<String> splitCodes(String raw) {
    List<String> out = new ArrayList<>();
    for (String part : raw.split("[,;\\s]+")) {
      String c = part.trim();
      if (!c.isEmpty()) {
        out.add(c.length() > 32 ? c.substring(0, 32) : c);
      }
    }
    return out;
  }

  private static List<Map<String, String>> demoSuggestions() {
    List<Map<String, String>> list = new ArrayList<>();
    list.add(row("DEMO-CHG-100", "CHARGE", "120"));
    list.add(row("DEMO-CHG-200", "CHARGE", "80"));
    list.add(row("X99.1", "DIAGNOSIS", "0"));
    list.add(row("Z00-DEMO", "DIAGNOSIS", "0"));
    return list;
  }

  private static Map<String, String> row(String code, String type, String amountYuan) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("code", code);
    m.put("type", type);
    m.put("amountYuan", amountYuan);
    return m;
  }
}
