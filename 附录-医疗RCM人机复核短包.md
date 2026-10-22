# 医疗后台 RCM 人机复核短包 · 渠道挂单策略（接单助手 2026-10-22）

> 基于已有 **Agent 审计护栏** Demo 垂直化：收费项/诊断码建议必须人工确认闸 + 审计日志。  
> **Han 本人在接单助手确认「转」（2026-10-22 ~21:03 Asia/Shanghai）**。  
> 声明：**不做诊疗、不碰患者隐私实盘**；Demo 用虚构编码与金额。

## 决策（对齐 MCP Apps，不乱投）
- **Upwork**：不新建付费 Catalog；英文话术用于匹配 RCM / healthcare coding / human-in-the-loop audit 类岗位提案。可叠现有作品集 [spring-ai-mcp-apps-demo](https://github.com/han-jilong/spring-ai-mcp-apps-demo) 叙事（公开仓尚未含 RCM 代码，等韩「推」）。
- **Fiverr**：英文草稿文案已备（保持 Draft，不发布、不推 ID），除非韩另说「发」。Box 常 403，优先 Han Mac 粘贴。
- **程序员客栈**：不冷挂；派单对口再用中文一句话；App 沟通前仍先问 Han。

## 包名
- 中文：医疗后台 RCM 人机复核短包（Spring / MCP 审计闸）
- English：`Healthcare RCM: AI Suggest + Human Confirm Gate + Audit Log (Spring)`

## 范围（固定）
| # | 交付 | 验收 |
|---|------|------|
| R1 | 只读建议列表 | `list-suggested-charges` / `get-claim-draft` 立即返回；审计 `ALLOWED_READ` |
| R2 | 写操作确认闸 | `propose-apply-charge-codes` 只发 `confirmToken`；`confirm-pending-action` 后才「入账」到 Demo 账本 |
| R3 | 金额/条数限额 | 软阈值提示 + 硬限额直接 DENIED（数字可改） |
| R4 | 审计 JSONL | who / tool / codes 摘要 / decision / ts；重启可查 |
| R5 | 联调讲解 | 30–45 分钟：建议→PENDING→APPROVED/DENIED |

## 不含
- ❌ 真实 HIS/医保接口、生产患者数据、等保陪跑、包月运维
- ❌ 诊疗决策、自动提交真实理赔
- ❌ 默认 OAuth（另册）

## 参考报价
**¥3,000 – ¥6,000** · **5–7 工作日**（固定价参考，非保证成交）

## Fiverr draft（English，fixed — Draft only）
**Title:** Healthcare RCM Demo: AI Code Suggestions + Human Confirm + Audit (Spring)

**Packages (example):**
- Basic $199 — Runnable demo + checklist + 30-min walkthrough (5 days)
- Standard $349 — Custom soft/hard limits + CSV export (5–7 days)

**Description (short):**
Fixed-scope Spring demo: suggested charge/diagnosis codes stay PENDING until confirmToken; append-only audit log. No real PHI, no live clearinghouse. Not monthly ops. Local demo ready; fictional DEMO-CHG / X99.1 samples only.

## 客栈一句话
医疗后台 RCM：AI 建议收费/编码必须人工确认闸 + 审计日志，Spring 短包 5–7 天参考 ¥3–6K；Demo 无真实病患数据；不含包月。

## Upwork proposal hook
Healthcare RCM vertical on Spring: AI suggests charge/diagnosis codes → human confirm gate (`confirmToken`) → append-only audit log. Fixed scope, fictional data only (no PHI / no live HIS). Demo locally runnable; portfolio narrative stacks with spring-ai-mcp-apps-demo. ¥3–6k / 5–7 days equivalent fixed package.

## 源码挂点
- 路径：`/workspace/spring-ai-mcp-apps-demo/`
- 类：`GuardedRcmTools` + 共用闸 `confirm-pending-action` / `get-audit-log`
- 大纲：`docs/rcm-guard-outline.md` · 附录：`附录-医疗RCM人机复核短包.md`
- 测试：`GuardedRcmToolsTest`（`./gradlew test`）
- 公开仓尚未含 RCM（等韩「推」）；本地可演示即可用话术

## 状态
2026-10-22 ~21:03 Asia/Shanghai：Han 在接单助手确认「转」→ 渠道口径已挂（Upwork 话术就绪 / Fiverr Draft 备好不发布 / 客栈不冷挂）。未花 Connects、未建 Catalog、未推 ID/W-9、未推公开仓。
