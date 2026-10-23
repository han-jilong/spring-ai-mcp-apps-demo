> **同源副本**：主策略见 `/workspace/freelance-packages/Agent资金人审闸-短包.md`（本文件与之同步备货，便于仓库内查阅）。

# Agent 资金人审闸短包 · 渠道挂单策略（备货 2026-10-23）

> 对齐 [Stripe Treasury for agents](https://stripe.com/newsroom/news/treasury-for-agents)（2026-10-21）公开叙述：**读余额可自动，动钱（付发票 / 转账）默认要人审**。  
> 垂直化已有 **ConfirmationGate + GuardedAccountTools**（可叠 GuardedRcmTools 叙事）。  
> **风险一句**：大陆个人难直接用 Stripe Treasury；作品集用**沙箱 / 海外主体**叙事；**稳定币实盘有合规红线**——本包只做权限闸 Demo，不承诺境内可开户、不代付、不碰实盘资金。  
> **状态：已挂渠道 + 本附录已推公开仓**（2026-10-23 Asia/Shanghai；韩确认挂后接单助手执行；代码 GuardedAccountTools 早已在 main）。

## 决策（对齐 MCP Apps，不乱投）
- **Upwork**：不新建付费 Catalog；英文话术用于匹配 agent treasury / payment approval / human-in-the-loop money move / MCP audit 类岗位提案。叠作品集 [spring-ai-mcp-apps-demo](https://github.com/han-jilong/spring-ai-mcp-apps-demo)。
- **Fiverr**：英文草稿文案已备（保持 **Draft，不发布、不推 ID**），除非韩另说「发」。Box 常 403，优先 Han Mac 粘贴。
- **程序员客栈**：不冷挂；派单对口再用中文一句话；App 沟通前仍先问 Han。
- 未花 Connects、未推身份证 / W-9、未保证收益。

## 包名
- 中文：Agent 资金人审闸短包（读余额自由 · 动钱 confirmToken · 审计）
- English：`Agent Treasury Guard: Balance Read-Free + Send-Money Confirm Gate + Audit (Spring / MCP)`

## 范围（固定）
| # | 交付 | 验收 |
|---|------|------|
| T1 | 只读余额 | `get-demo-balance`（叙事名可称 `get-balance-summary`）立即返回；审计 `ALLOWED_READ` |
| T2 | 动钱提议闸 | `propose-demo-transfer` / 叙事名 `propose-send-money` **只签发** `confirmToken`（TTL≤10min）；**不**改余额 |
| T3 | 确认后落地 | `confirm-pending-action` 消费 token 后才扣/转 Demo 账本；无效/过期 → `DENIED` |
| T4 | 金额限额 | 软阈值提示 + 硬限额直接 `DENIED`（token 不可绕过；数字可改） |
| T5 | 审计 JSONL | who / tool / 金额摘要 / decision / ts；决策 ∈ `ALLOWED_READ\|PENDING\|APPROVED\|DENIED`；重启可查 |
| T6 | 联调讲解 | 30–45 分钟：查余额 → PENDING → APPROVED/DENIED（含超硬限额） |

**Demo 复用**：现有 `GuardedAccountTools` + 共用闸即可演示；**可选**后续再把 tool 对外叙事名改成更贴 Stripe 的 `propose-send-money` / `get-balance-summary`（不改 Java 也能挂单讲故事）。

官方能力对照（叙事引用，非本包集成）：[Stripe Treasury agents docs](https://docs.stripe.com/treasury/agents)（如 `get_balance_summary`、`send_money` 与人审/权限模型）。

## 不含（Out of scope）
- ❌ 真实 Stripe Treasury **开户** / 生产密钥对接 / 正式 KYC
- ❌ 承诺**大陆个人**可开 Treasury 或境内可收款
- ❌ 包月运维、7×24、线上 SLA
- ❌ **稳定币 / x402 实盘**付款、代付 token / 代持钱包
- ❌ 替客户真实付发票、转账、跨境清算
- ❌ 默认 OAuth（另册）；生产级 WORM / SIEM（可另议二期）

## 参考报价
**¥2,000 – ¥5,000** · **3–6 工作日**（固定价参考，**非保证成交**）

| 档位 | 典型内容 |
|------|----------|
| 偏下限 | 复用 GuardedAccountTools 演示路径 + 验收清单 + 讲解 |
| 中档 | 上项 + 按客户软/硬限额数字 + 审计导出（JSON/CSV） |
| 接近上限 | 上项 + tool 对外命名对齐 Stripe 叙事 + 与客户现有 money tool 名单对接（仍无真实开户） |

## Fiverr draft（English，fixed — Draft only）
**Title:** Agent Money Guard: Balance OK Auto + Send Needs Human Confirm (Spring MCP)

**Packages (example):**
- Basic $149 — Runnable demo + checklist + 30-min walkthrough (3–4 days)
- Standard $299 — Custom soft/hard limits + audit export (5–6 days)

**Description (short):**
Fixed-scope Spring/MCP demo aligned with “read balance free, move money needs approval”: `propose-send-money` returns `confirmToken` only; confirm then applies to a **sandbox ledger**. Append-only audit JSONL. No real Stripe Treasury onboarding, no mainland account promise, no live stablecoin, no monthly ops. Portfolio: spring-ai-mcp-apps-demo.

## 客栈一句话
Agent 资金人审闸：查余额自由、转账/付发票必须 confirmToken 人审 + 审计 JSONL；Spring 短包 3–6 天参考 ¥2–5K；沙箱 Demo，不含真实 Stripe 开户/稳定币实盘/包月。

## Upwork proposal hook
Agent treasury guard on Spring MCP: balance/read tools → `ALLOWED_READ`; send-money / transfer stays `PENDING` until `confirmToken`; hard amount limits + append-only audit. Matches Stripe’s public “approve money movement” model (sandbox narrative only—no live Treasury onboarding). Demo in https://github.com/han-jilong/spring-ai-mcp-apps-demo (`GuardedAccountTools`). Fixed package ¥2–5k / 3–6 days equivalent. Not monthly ops.

## 源码挂点
- 路径：`/workspace/spring-ai-mcp-apps-demo/`
- 类：`GuardedAccountTools` + `ConfirmationGate` + `AuditLogStore`；共用 `confirm-pending-action` / `get-audit-log`
- 现成 tool：`get-demo-balance`、`propose-demo-transfer`、`propose-set-account-note`
- 大纲：`docs/audit-guard-outline.md` · 附录：`附录-Agent资金人审闸短包.md`
- 公开仓：https://github.com/han-jilong/spring-ai-mcp-apps-demo
- 引用：[newsroom](https://stripe.com/newsroom/news/treasury-for-agents) · [docs](https://docs.stripe.com/treasury/agents)

## 状态
**已挂渠道 + 附录已推**（2026-10-23 Asia/Shanghai）— 韩确认挂后接单助手按固定范围挂；GuardedAccountTools 已在 main；本附录随 README 指针一并推送。不含真实 Stripe 开户/稳定币实盘。
