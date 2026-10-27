Spring AI MCP Apps Server
===
This example highlight how to use metadata on an MCP server's tools and
resources to enable [MCP Apps](https://modelcontextprotocol.io/extensions/apps/overview).

Building and Running the Server
---
This example is built with Gradle. There is a Gradle wrapper available in the
project directory, so you can build an executable JAR file like this:

```
./gradlew build
```

Then you can run it like this:

```
java -jar build/libs/mcp-apps-server-0.0.1-SNAPSHOT.jar
```

Optionally, you can use the Spring Boot Gradle plugin to build and run the
app like this:

```
./gradlew bootRun
```

Or, if you want, run the application through your IDE using the facilities
of your favorite IDE.

Once the application starts, the MCP server will be listening on port
3001.

Using the MCP App Server
---
To see the MCP App in action, you'll need to configure the server in an
MCP client that supports MCP Apps. This includes MCP Jam and Claude
Desktop. Goose also supports MCP Apps, but does not (yet, as of version
1.27.2) support the ability for the app to update the context model, so
at least for now you cannot use Goose.

When you configure the MCP server, choose Streamable HTTP as the transport
protocol and set the URL to "http://localhost:3001/mcp".

Claude Desktop (at least as of version 1.1.5749) does not support Streamable
HTTP. But you can still use a Streamable HTTP MCP server by using the
mcp-remote (STDIO transport) MCP server to proxy to the Streamable HTTP
server. Configure it in the Claude Desktop configuration JSON like this:

```
"mcpServers": {
  "dice-tools": {
    "command": "npx",
    "args": [
      "-y",
      "mcp-remote",
      "http://localhost:3001/mcp"
    ]
  }
}
```

After configuring the MCP server in your MCP client, type "Roll the dice"
in the chat. If everything is configured correctly, after a moment you
should see a pair of dice, rolled to some values. Then ask "What was the
result?" to verify that the application was about to update the model
context. Then click the "Roll Dice" button to re-roll the dice and ask
again.

Agent audit guardrail (optional demo tools)
---
Alongside the dice MCP App, this repo includes a **minimal write-confirm + audit** demo
(no OAuth by default; does not change the dice UI):

- Read-only tools (`list-account-notes`, `get-demo-balance`, `get-audit-log`) run immediately.
- Mutating tools (`propose-set-account-note`, `propose-demo-transfer`) return a `confirmToken`;
  call `confirm-pending-action` to apply.
- **Amount limits (CNY):** soft threshold **100** → PENDING message flags over-soft; hard limit **10000** → `DENIED` immediately (token cannot bypass).
- Audit: append-only decisions `ALLOWED_READ` / `PENDING` / `APPROVED` / `DENIED`. In-memory ring (max 500) + JSONL file `./data/audit-log.jsonl` (survives restart; demo-grade, not WORM).

Sales appendix (Chinese): `附录-Agent审计护栏短包.md` · outline: `docs/audit-guard-outline.md`.

Agent treasury / send-money human-confirm narrative (same tools; sandbox ledger only—no live Stripe Treasury):
Sales appendix: `附录-Agent资金人审闸短包.md`.

### Dual path: MCP confirmToken + Spring AI 2.1 Tool Call Confirmation
- **MCP path (unchanged):** `GuardedAccountTools` `@McpTool` + `ConfirmationGate` explicit `confirmToken` (propose → `confirm-pending-action`).
- **ChatClient path (2.1+):** `TreasuryChatTools` `@Tool(requiresConfirmation=true)` on mutating tools (`demoTransfer`, `setAccountNote`); read-only stay ungated. App resumes with `ToolCallDecisions.allow` / `deny` and `ToolCallingManager.executeToolCalls(...)`.
- Boot props: `spring.ai.tools.confirmation.enabled` / `spring.ai.tools.confirmation.tools` (see `application.properties`).
- Versions: Spring AI **2.1.0-RC1**, Spring Boot **4.2.0-RC1**. Docs: [Tool Call Confirmation](https://docs.spring.io/spring-ai/reference/2.1/api/tools/tool-call-confirmation.html). Sandbox ledger only.

Quick verify after `bootRun` (MCP client or any tools/call): read → propose → confirm → `get-audit-log`.

RCM vertical demo (fictional healthcare coding)
---
Alongside the account audit tools, **GuardedRcmTools** demos a healthcare RCM human-confirm gate
(no PHI, no live insurance / clearinghouse; not clinical advice):

- Read-only: `list-suggested-charges`, `get-claim-draft` → immediate + audit `ALLOWED_READ` (patient label `DEMO-PATIENT`, codes like `DEMO-CHG-100` / `X99.1`).
- Mutating: `propose-apply-charge-codes` → `PENDING` + `confirmToken` only; apply via existing `confirm-pending-action`.
- Limits (CNY / count): soft amount **500** / soft code count **5** → PENDING cue; hard amount **10000** / hard code count **20** → `DENIED`.
- Reuses shared `ConfirmationGate` + `AuditLogStore` JSONL; does not change the Dice MCP Apps UI.

Sales appendix: `附录-医疗RCM人机复核短包.md` · outline: `docs/rcm-guard-outline.md`.
Status: public demo includes RCM vertical (fictional data only; no PHI).

作品集 MCP 最小演示
---
面向面试/接单的可点路径（输入 → 工具调用 → 可选人审 → 审计），不讲概念。

### 本骨架提供什么
- 包：`com.example.mcpappsserver.portfolio.PortfolioMcpTools`
- 只读 `@McpTool`（无副作用，带 `readOnlyHint`）：
  - `portfolio-health` — 探活
  - `portfolio-echo` — 回显入参（烟雾测试）
  - `portfolio-list-demo-accounts` — 固定沙箱账户目录
  - `portfolio-policy-eligibility-demo` — 沪青年租补**演示**资格预检（硬编码 3 条规则；非真政策）
- 每次调用写审计 `ALLOWED_READ`（复用 `AuditLogStore` JSONL）。
- **可选人审**：同仓已有 `GuardedAccountTools`（`propose-*` → `confirmToken` → `confirm-pending-action`）+ `ConfirmationGate`；本骨架不重复写可变工具，避免破坏现有测试。

Client 侧：本仓是 **MCP Server**；作品集演示用 **MCP Inspector** 当 Client（无需再起 Ollama/第二个 Boot 模块）。完整 Spring AI MCP Client + ChatClient 可另开仓按 [HowToDoInJava](https://howtodoinjava.com/spring-ai/spring-ai-mcp/) 加。

### 如何启动
```bash
./gradlew bootRun
# 或
./gradlew build && java -jar build/libs/mcp-apps-server-0.0.1-SNAPSHOT.jar
```
监听 `http://localhost:3001/mcp`（Streamable HTTP）。

### 如何用 MCP Inspector（当 Client）
需本机 Node.js。先 `bootRun`，再：

```bash
# 列出工具（应看到 portfolio-* 与既有 guard/dice 工具）
npx -y @modelcontextprotocol/inspector --cli http://localhost:3001/mcp --transport http --method tools/list

# 调用只读骨架
npx -y @modelcontextprotocol/inspector --cli http://localhost:3001/mcp --transport http --method tools/call --tool-name portfolio-health
npx -y @modelcontextprotocol/inspector --cli http://localhost:3001/mcp --transport http --method tools/call --tool-name portfolio-echo --tool-arg message=hello
npx -y @modelcontextprotocol/inspector --cli http://localhost:3001/mcp --transport http --method tools/call --tool-name portfolio-list-demo-accounts
npx -y @modelcontextprotocol/inspector --cli http://localhost:3001/mcp --transport http --method tools/call --tool-name portfolio-policy-eligibility-demo --tool-arg age=28 --tool-arg hasShanghaiWorkOrHukou=true --tool-arg monthlyRentCny=3000 --tool-arg monthlyIncomeCny=10000

# 可选人审闭环（已有工具）
npx -y @modelcontextprotocol/inspector --cli http://localhost:3001/mcp --transport http --method tools/call --tool-name get-demo-balance
# propose-demo-transfer → 记下 confirmToken → confirm-pending-action → get-audit-log
```

浏览器 UI：去掉 `--cli` 打开 Inspector。

### 与人审闸的关系
| 步骤 | 工具 | 闸 |
|------|------|----|
| 输入→只读调用 | `portfolio-*` / `list-account-notes` / `get-demo-balance` | 无；审计 `ALLOWED_READ` |
| 可变提议 | `propose-demo-transfer` 等 | `ConfirmationGate` 发 `confirmToken`（`PENDING`） |
| 人审确认 | `confirm-pending-action` | 消费 token → `APPROVED` / 无效则 `DENIED` |
| 审计回看 | `get-audit-log` | 只读 |

ChatClient 双路径（`TreasuryChatTools` + Spring AI 2.1 `requiresConfirmation`）见上文「Dual path」；与 MCP confirmToken 并存，互不替换。

### 部署安全清单（演示级提醒）
本地演示 `/mcp` **默认无鉴权**。对外前请：Spring Security / OAuth 护 `/mcp`、按调用方授权、服务端校验入参、勿把真实资金/PHI 挂上。详见教程文末与本仓 `附录-MCP-OAuth鉴权短包.md`。

版本：Spring AI **2.1.0-RC1** · Boot **4.2.0-RC1** · Java 21。

