# 挂单 hook 草稿 · MCP 按域拆分 + 人审闸（2026-10-29）

> 本地备货 · **未改公开仓**（等韩「推」/接单侧触发再贴）。对齐现有 demo：`ConfirmationGate` + `GuardedAccountTools` + `PortfolioMcpTools`（ae9364a）。

## Upwork / 英文一句话
Spring MCP demo: **tools split by domain** (read-only portfolio/eligibility vs money-move) + **human confirm gate**—balance/read returns immediately; send/transfer only after `confirmToken`. Audit JSONL. Fixed package, no monthly ops. Portfolio: https://github.com/han-jilong/spring-ai-mcp-apps-demo

## 客栈 / 中文一句话
MCP **按域拆工具**（只读业务 vs 动钱）+ **人审闸**：查余额自由、转账须 confirmToken；Spring 固定短包，沙箱 Demo，不含包月/真实开户。仓：spring-ai-mcp-apps-demo

## 与交付页关系
客户长说明见 `Agent资金人审闸-交付页.md`；本文件只供提案 hook 粘贴。

## 风险一句
沙箱叙事；不承诺大陆可开海外 Treasury；不碰实盘资金。
