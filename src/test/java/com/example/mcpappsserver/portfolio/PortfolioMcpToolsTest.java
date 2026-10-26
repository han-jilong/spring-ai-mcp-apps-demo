package com.example.mcpappsserver.portfolio;

import com.example.mcpappsserver.audit.AuditEntry;
import com.example.mcpappsserver.audit.AuditLogStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Portfolio MCP skeleton: read-only tools + audit ALLOWED_READ (no Spring / no wire).
 */
class PortfolioMcpToolsTest {

  @TempDir
  Path tmp;

  @Test
  void healthEchoAndListAccountsAreReadOnlyAndAudited() throws Exception {
    Path logFile = tmp.resolve("audit-log.jsonl");
    AuditLogStore store = newStore(logFile);
    PortfolioMcpTools tools = new PortfolioMcpTools(store);

    String health = tools.portfolioHealth();
    assertTrue(health.contains("status=ok"), health);
    assertTrue(health.contains("portfolio-mcp-skeleton"), health);

    String echo = tools.portfolioEcho("  hello-mcp  ");
    assertEquals("echo=hello-mcp", echo);

    String accounts = tools.portfolioListDemoAccounts();
    assertTrue(accounts.contains("ACC-DEMO-001"), accounts);
    assertTrue(accounts.contains("ACC-DEMO-002"), accounts);

    List<AuditEntry> rows = store.recent(10);
    assertTrue(rows.stream().anyMatch(e ->
        "portfolio-health".equals(e.toolName()) && "ALLOWED_READ".equals(e.decision())));
    assertTrue(rows.stream().anyMatch(e ->
        "portfolio-echo".equals(e.toolName()) && "ALLOWED_READ".equals(e.decision())));
    assertTrue(rows.stream().anyMatch(e ->
        "portfolio-list-demo-accounts".equals(e.toolName()) && "ALLOWED_READ".equals(e.decision())));
    assertEquals(3, rows.stream().filter(e -> "ALLOWED_READ".equals(e.decision())).count());
  }

  @Test
  void echoTruncatesLongMessage() throws Exception {
    AuditLogStore store = newStore(tmp.resolve("audit2.jsonl"));
    PortfolioMcpTools tools = new PortfolioMcpTools(store);
    String longMsg = "x".repeat(250);
    String echo = tools.portfolioEcho(longMsg);
    assertEquals(205, echo.length()); // "echo=" + 200
    assertTrue(echo.startsWith("echo="));
  }

  private static AuditLogStore newStore(Path file) throws Exception {
    var ctor = AuditLogStore.class.getDeclaredConstructor(Path.class);
    ctor.setAccessible(true);
    return ctor.newInstance(file);
  }
}
