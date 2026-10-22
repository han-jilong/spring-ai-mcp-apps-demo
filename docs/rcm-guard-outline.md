# 医疗 RCM 人机复核 · 插接大纲（本仓库 Demo）

> 配合 `附录-医疗RCM人机复核短包.md`。叠在 Agent 审计护栏之上；**无 PHI / 无真实医保**；不改 Dice UI。  
> 状态：本地可演示；**等 Han「转 RCM」**再交接单助手。

## 组件

| 类 | 职责 |
|----|------|
| `GuardedRcmTools` | RCM 只读建议 + `propose-apply-charge-codes`；虚构码与 `DEMO-PATIENT` |
| `audit.ConfirmationGate` | 与账户 Demo **共用** propose → confirmToken |
| `audit.AuditLogStore` | 共用 JSONL `./data/audit-log.jsonl` |
| `GuardedAccountTools.confirm-pending-action` | 通用确认闸（RCM PENDING 也走它） |
| `GuardedAccountTools.get-audit-log` | 共用审计只读 |

## Tool 一览

| Tool | 决策 |
|------|------|
| `list-suggested-charges` | `ALLOWED_READ` |
| `get-claim-draft` | `ALLOWED_READ` |
| `propose-apply-charge-codes` | `PENDING` / `DENIED`（硬限额） |
| `confirm-pending-action`（已有） | `APPROVED` / `DENIED` |
| `get-audit-log`（已有） | `ALLOWED_READ` |

## 限额（相对账户 Demo 略不同）

- 软金额 `SOFT_AMOUNT_THRESHOLD = 500` CNY；硬金额 `HARD_AMOUNT_LIMIT = 10000` CNY
- 软条数 `SOFT_CODE_COUNT = 5`；硬条数 `HARD_CODE_COUNT = 20`

## 声明

不做诊疗、不碰患者隐私实盘；样例码 `DEMO-CHG-*` / `X99.1` 等均为虚构。

## 验证

`./gradlew test`（含 `GuardedRcmToolsTest`）→ `bootRun` → 建议列表 → propose → confirm → `get-audit-log`。
