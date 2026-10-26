package com.example.mcpappsserver.portfolio;

import com.example.mcpappsserver.audit.AuditLogStore;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
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
}
