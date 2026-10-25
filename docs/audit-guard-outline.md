# Agent 审计护栏 · 插接大纲（本仓库 Demo）

> 配合 `附录-Agent审计护栏短包.md`。默认启用、**无 OAuth**、不改 Dice MCP Apps UI。

## 组件

| 类 | 职责 |
|----|------|
| `audit.AuditEntry` | 一行：id / time / actor / tool / argsSummary / decision / detail |
| `audit.AuditLogStore` | 内存环（max 500）+ JSONL 落盘 `./data/audit-log.jsonl`（重启可恢复；可换 H2/SQLite） |
| `audit.ConfirmationGate` | propose → confirmToken（TTL 10min）→ confirm 执行一次 |
| `GuardedAccountTools` | 只读立刻放行；写操作走 propose + `confirm-pending-action` |

## 决策枚举（demo）

- `ALLOWED_READ` — 只读 tool
- `PENDING` — 已签发 confirmToken，尚未执行
- `APPROVED` — confirm 成功并落地
- `DENIED` — 参数非法 / token 无效 / 超硬限额

## 金额叙事

- 软阈值 `CONFIRM_THRESHOLD = 100` CNY：PENDING 文案提示 over-soft（仍须 confirm）
- 硬限额 `HARD_LIMIT = 10000` CNY：直接 `DENIED`，confirmToken **无法绕过**

## 与 OAuth 附录关系

OAuth（`附录-MCP-OAuth鉴权短包.md`）管**谁能调**；本护栏管**调了写操作是否二次确认 + 审计**。可叠卖，默认互不打开对方。

## 验证（无 key）

见 README「Agent 审计护栏」节：起服后用 MCP client / curl 调 `list-account-notes` → `propose-*` → `confirm-pending-action` → `get-audit-log`。


## Dual path（2026-10-25）：MCP confirmToken + Spring AI 2.1 official confirmation

| 路径 | 面向 | 机制 | 类 |
|------|------|------|----|
| MCP | MCP clients / `@McpTool` | `ConfirmationGate` 显式 `confirmToken`（propose → confirm-pending-action） | `GuardedAccountTools` |
| ChatClient | `ChatClient` / `@Tool` | Spring AI **2.1+** `@Tool(requiresConfirmation=true)` → `ToolCallConfirmationPolicy` → `ToolCallDecisions.allow/deny` → `ToolCallingManager.executeToolCalls` | `TreasuryChatTools` |

- 只读 tool 两路都不闸；动钱 / 写 note 才闸。
- Boot 属性（ChatClient 路）：`spring.ai.tools.confirmation.enabled`、`spring.ai.tools.confirmation.tools`（见 `application.properties`）。
- 官方文档：https://docs.spring.io/spring-ai/reference/2.1/api/tools/tool-call-confirmation.html
- 依赖：`springAiVersion=2.1.0-RC1` + Boot `4.2.0-RC1`；沙箱账本，无真实 Stripe/支付宝。
