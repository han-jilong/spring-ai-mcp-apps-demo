package com.example.mcpappsserver.portfolio;

import com.example.mcpappsserver.audit.AuditLogStore;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作品集用最小 MCP 工具骨架（只读、无副作用）。
 * <p>
 * 演示路径：Inspector / MCP Client 调用本类 tools → 审计 {@code ALLOWED_READ}。
 * 可选人审（propose → confirmToken）请用同仓 {@code GuardedAccountTools}，勿在此写可变操作。
 * 大陆可达叙事；沙箱假数据；不卖期货/支付信号。
 */
@Service
public class PortfolioMcpTools {

  private final AuditLogStore auditLog;

  /** Fixed demo accounts — read-only catalog for portfolio demos. */
  private final Map<String, String> demoAccounts = new LinkedHashMap<>();

  public PortfolioMcpTools(AuditLogStore auditLog) {
    this.auditLog = auditLog;
    demoAccounts.put("ACC-DEMO-001", "sandbox checking · CNY");
    demoAccounts.put("ACC-DEMO-002", "sandbox savings · CNY");
    demoAccounts.put("ACC-DEMO-003", "sandbox escrow · CNY");
  }

  @McpTool(
      name = "portfolio-health",
      title = "Portfolio MCP health (read-only)",
      description = "Read-only: return ok + server role for portfolio demos. No confirmation required.",
      annotations = @McpTool.McpAnnotations(
          readOnlyHint = true,
          destructiveHint = false,
          idempotentHint = true,
          openWorldHint = false))
  public String portfolioHealth() {
    String body = "status=ok role=portfolio-mcp-skeleton springAi=2.1.0-RC1";
    auditLog.append("agent", "portfolio-health", "", "ALLOWED_READ", body);
    return body;
  }

  @McpTool(
      name = "portfolio-echo",
      title = "Portfolio echo (read-only)",
      description = "Read-only: echo a short message back (input → tool call smoke test). No confirmation required.",
      annotations = @McpTool.McpAnnotations(
          readOnlyHint = true,
          destructiveHint = false,
          idempotentHint = true,
          openWorldHint = false))
  public String portfolioEcho(
      @McpToolParam(description = "Message to echo (max 200 chars)", required = true) String message) {
    String safe = message == null ? "" : message.trim();
    if (safe.length() > 200) {
      safe = safe.substring(0, 200);
    }
    String body = "echo=" + safe;
    auditLog.append("agent", "portfolio-echo", "len=" + safe.length(), "ALLOWED_READ", body);
    return body;
  }

  @McpTool(
      name = "portfolio-list-demo-accounts",
      title = "List demo accounts (read-only)",
      description = "Read-only: list fixed sandbox demo account ids/labels. No confirmation required. Not real banking.",
      annotations = @McpTool.McpAnnotations(
          readOnlyHint = true,
          destructiveHint = false,
          idempotentHint = true,
          openWorldHint = false))
  public String portfolioListDemoAccounts() {
    String summary = demoAccounts.entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("; "));
    String body = "demoAccounts: " + summary;
    auditLog.append("agent", "portfolio-list-demo-accounts", "", "ALLOWED_READ",
        "count=" + demoAccounts.size());
    return body;
  }

  /**
   * Demo-only 「沪青年租补」资格预检：硬编码 3 条演示规则，非真实政策/非申报通道。
   */
  @McpTool(
      name = "portfolio-policy-eligibility-demo",
      title = "沪青年租补资格演示预检 (read-only)",
      description = "Read-only DEMO: hardcoded Shanghai youth rent-subsidy style rules (age/workOrHukou/rentVsIncome). Returns possible-match + disclaimer. NOT real policy; not a filing channel. No confirmation required.",
      annotations = @McpTool.McpAnnotations(
          readOnlyHint = true,
          destructiveHint = false,
          idempotentHint = true,
          openWorldHint = false))
  public String portfolioPolicyEligibilityDemo(
      @McpToolParam(description = "Age in years (integer)", required = true) Integer age,
      @McpToolParam(description = "true if has Shanghai hukou or local work/residence credential (demo flag)", required = true)
          Boolean hasShanghaiWorkOrHukou,
      @McpToolParam(description = "Monthly rent CNY", required = true) Integer monthlyRentCny,
      @McpToolParam(description = "Monthly take-home income CNY", required = true) Integer monthlyIncomeCny) {

    List<String> pass = new ArrayList<>();
    List<String> fail = new ArrayList<>();

    // Rule 1 (demo): youth band 18–35
    if (age != null && age >= 18 && age <= 35) {
      pass.add("R1-age: " + age + " in [18,35]");
    }
    else {
      fail.add("R1-age: need 18–35 (got " + age + ")");
    }

    // Rule 2 (demo): local work/hukou flag
    if (Boolean.TRUE.equals(hasShanghaiWorkOrHukou)) {
      pass.add("R2-local: shanghai work/hukou flag=true");
    }
    else {
      fail.add("R2-local: need hasShanghaiWorkOrHukou=true");
    }

    // Rule 3 (demo): rent burden — rent > 0, income > 0, rent/income <= 40%, rent <= 4500
    int rent = monthlyRentCny == null ? -1 : monthlyRentCny;
    int income = monthlyIncomeCny == null ? -1 : monthlyIncomeCny;
    if (rent > 0 && income > 0 && rent <= 4500 && rent * 100 <= income * 40) {
      pass.add("R3-burden: rent=" + rent + " income=" + income + " within demo caps");
    }
    else {
      fail.add("R3-burden: need rent>0, income>0, rent<=4500, rent/income<=40% (got rent="
          + monthlyRentCny + " income=" + monthlyIncomeCny + ")");
    }

    boolean possible = fail.isEmpty();
    String args = "age=" + age + ",local=" + hasShanghaiWorkOrHukou
        + ",rent=" + monthlyRentCny + ",income=" + monthlyIncomeCny;
    String body = "possibleMatch=" + possible
        + " passed=[" + String.join("; ", pass) + "]"
        + " failed=[" + String.join("; ", fail) + "]"
        + " DISCLAIMER=演示规则非真实沪青年租补政策，不能用于申报或承诺；请以官方渠道为准。";
    auditLog.append("agent", "portfolio-policy-eligibility-demo", args, "ALLOWED_READ",
        "possibleMatch=" + possible);
    return body;
  }

}
