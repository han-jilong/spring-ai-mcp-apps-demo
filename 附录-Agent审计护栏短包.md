# 附录 · 可选加购：Agent 审计护栏短包

> **可选 / 加购**，不改动主包「MCP Apps 交互面板短包」的固定价与边界（¥2.5–6k / 3–7 工作日）。  
> 对齐公开叙述：工具权限、**写操作人工/显式确认闸**、**金额限额**、审计日志（参见 DEV 文：[Agentic AI in practice — safe auditable agents with Java & Spring Boot](https://dev.to/said_olano/agentic-ai-in-practice-building-safe-auditable-agents-with-java-and-spring-boot-46k8)，2026-10-20）。  
> **里程碑栈**（Spring AI 2.0.x / Boot 4.x）——**勿对外宣称 GA / 生产已稳**。  
> 报价供挂单参考，**非保证成交**；Demo **无 key、默认可起服**；**不默认打开 OAuth**（OAuth 另见 `附录-MCP-OAuth鉴权短包.md`，等客户/你方说「转 OAuth」再上）。

---

## 1. 为什么卖（一句话）

MCP Apps 主包证明「聊天里能嵌面板」；客户下一步常问：「Agent 乱调写工具怎么办？」  
本加购交付**可演示**的三件套：只读自由、写操作 **confirmToken 闸**、append-only **审计**（含金额硬/软限额），挂在现有 MCP Server 旁，强化「可审计 Agent」叙事。

---

## 2. 包名 / 挂单标题

**中文**：Agent 审计护栏短包（MCP Apps / Spring AI 加购）

**English（Upwork / Fiverr）**：  
`Spring AI MCP: Read-Free Tools + Write Confirm Gate + Audit Log`

---

## 3. 范围（Scope）— 接单助手交付边界

| # | 交付物 | 验收口径（Acceptance） |
|---|--------|------------------------|
| S1 | **只读立刻放行** | `list-*` / `get-*` / `get-audit-log` 无 confirm；审计记 `ALLOWED_READ` |
| S2 | **写操作确认闸** | `propose-*` 只签发 `confirmToken`（TTL≤10min）；`confirm-pending-action` 消费后才落地；无效/过期 → `DENIED` |
| S3 | **金额限额** | 软阈值（默认 100 CNY）→ PENDING 文案提示；硬限额（默认 10000 CNY）→ 直接 `DENIED`（token 不可绕过）；数字可按客户改 |
| S4 | **审计日志** | who / tool / args 摘要 / decision / timestamp；决策 ∈ `ALLOWED_READ\|PENDING\|APPROVED\|DENIED`；Demo = 内存环 + JSONL 落盘（重启可查）；导出 JSON/CSV 为中档加项 |
| S5 | **不破坏主包 UI** | Dice MCP Apps 资源/工具可跑；护栏工具并列注册；**默认不开 OAuth** |
| S6 | **联调清单 + 讲解** | 30–45 分钟走通：只读 → PENDING → APPROVED / DENIED（含超硬限额）全路径 |

本仓库已落地最小 Demo：`GuardedAccountTools` + `audit.*`；大纲见 `docs/audit-guard-outline.md`。

---

## 4. 不含什么（Out of scope）— 防包月

- ❌ 包月运维、7×24、线上 SLA、完整合规认证 / 等保测评陪跑  
- ❌ 默认启用 OAuth / SSO（另册加购；本包验收**不**包含鉴权）  
- ❌ 替客户采购 LLM / 代付 token  
- ❌ 生产级防篡改审计（WORM、SIEM、不可抵赖签名）——可另议二期  
- ❌ 保证各 MCP 客户端对「二次确认」交互的统一 UX（协议与客户端仍在演进）  
- ❌ 对接客户现有全部 tool 名单的逐一改造（中档/上限档另计）

---

## 5. 验收清单（给接单助手勾选）

客户侧演示通过即验收（本地无 key）：

- [ ] `get-demo-balance` / `list-account-notes` 立即返回，审计有 `ALLOWED_READ`
- [ ] `propose-demo-transfer` 金额 ≤ 硬限额 → 返回 `PENDING` + `confirmToken`
- [ ] 金额 > 软阈值 → PENDING 文案含 soft-threshold 提示
- [ ] 金额 > 硬限额 → `DENIED`，无 token / 有 token 亦不可执行该笔
- [ ] `confirm-pending-action` 正确 token → `APPLIED` + 审计 `APPROVED`；错误/过期 → `DENIED`
- [ ] 重启进程后 `get-audit-log` 仍能看到落盘行（JSONL）
- [ ] Dice「Roll the dice」面板仍可用（主包未坏）
- [ ] 讲解纪要 / README 路径已交付

---

## 6. 参考报价（加购）

**¥2,000 – ¥4,000**（人民币，固定价 · **2–5 个工作日**）

| 档位 | 典型内容 |
|------|----------|
| 偏下限 | 确认闸 + JSONL/内存审计 + 与主包并列演示 + 讲解 |
| 中档 | 上项 + 金额硬/软限额按客户数字 + 审计导出（JSON/CSV） |
| 接近上限 | 上项 + 持久化升级（H2/SQLite）+ 与客户现有 tool 名单对接 |

与主包合并时：主包仍按 ¥2.5–6k；本加购**另计**。可与 OAuth 短包叠卖（谁能调 × 调了是否确认）。

---

## 7. 交付节奏（示例）

- **D1**：对齐只读/写 tool 名单、限额数字、审计字段  
- **D2–3**：闸 + 审计落地、与现有 MCP Apps Demo 联调  
- **D4–5**：文档、导出可选、远程讲解  

---

## 8. 大陆友好 / 零前期花费

| 项 | 口径 |
|----|------|
| **起草与本地 Demo** | 无付费 API key；本仓库 `./gradlew bootRun` 即可演示 |
| **OAuth** | **默认关闭**；需要时走 OAuth 附录，不混进本短包必选项 |
| **数据** | Demo 内存账本 + 本地 JSONL；客户生产库另议，不默认连外网 |

---

## 9. 技术挂点（摘要）

详见 `docs/audit-guard-outline.md`。要点：

1. `ConfirmationGate.propose` → `confirmToken`；`confirm` 单次消费。  
2. `AuditLogStore` append-only；decision ∈ `ALLOWED_READ|PENDING|APPROVED|DENIED`；JSONL 默认 `./data/audit-log.jsonl`。  
3. 新 `@McpTool` 放在独立 `@Service`，**不动** `DiceApp`。  
4. 可选二期：H2/SQLite、按 actor 分列、与 Spring Security method 边界对齐。

---

## 10. 一句话话术（给接单助手）

MCP Apps 面板之上，加一层 **Agent 审计护栏**：只读自由、写操作必须 **confirmToken**、金额软提示/硬拒、审计 JSONL 可重启回看——**2–5 天、参考 ¥2–4K**（可与主包/OAuth 搭售；里程碑栈；非保证成交；本地无 key 可演示）。

---

## 11. 相关链接

- 叙事参考：[Safe auditable agents · Java / Spring Boot（DEV，2026-10-20）](https://dev.to/said_olano/agentic-ai-in-practice-building-safe-auditable-agents-with-java-and-spring-boot-46k8)  
- 主包：`报价包.md` · OAuth：`附录-MCP-OAuth鉴权短包.md` · 大纲：`docs/audit-guard-outline.md`
