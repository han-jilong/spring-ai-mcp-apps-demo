# 附录 · 可选加购：鉴权加固 & GCP OpenAPI→MCP

> **可选 / 加购**，不改动主包「MCP Apps 交互面板短包」的固定价与边界（¥2.5–6k / 3–7 工作日）。  
> 需要时单独报价、单独写进范围；**非保证成交**。

---

## A. Spring MCP 鉴权 / 网关加固（推荐加购）

**卖点依据**：Spring AI MCP Server 文档明确指出，HTTP 类传输（SSE / Streamable-HTTP / Stateless）**默认无鉴权**——公网裸暴露 `/mcp` 等于谁都能调你的 tool。

文档：[MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)

**可交付（示例范围，另议）**：

- 在 `/mcp`（及同类端点）前加 **Spring Security**（API Key / Bearer 等约定一种），或  
- 用客户已有 **API 网关** 挡在 MCP 入口前（转发规则 + 鉴权头透传）  
- 配置要点一页 + 联调说明（仍为短包，不做包月运维）

适合：客户要把 Demo 从本机/内网挪到可远程访问环境时一并卖。

需要 **OAuth2 + 按 tool 的 scope + 参数校验** 的完整短包（可单独挂单，参考 ¥2–5k / 3–7 天）见同目录 `附录-MCP-OAuth鉴权短包.md`；插接位置见 `docs/oauth-outline.md`。

---

## B. GCP OpenAPI → MCP（相邻短注 · Public Preview）

Google Cloud **API Gateway** 支持把已有 **OpenAPI 3.x** 标成 MCP 工具（`x-google-api-management.mcp` / `x-google-mcp-tool`），远程暴露 `tools/list` / `tools/call`，不必单独养一套 MCP 中间件。

- 介绍：[Turn REST APIs into MCP tools](https://developers.googleblog.com/en/turn-your-rest-apis-into-mcp-tools-with-google-cloud-api-gateway/)  
- 配置：[MCP configure](https://docs.cloud.google.com/api-gateway/docs/mcp-configure) · [MCP overview](https://docs.cloud.google.com/api-gateway/docs/mcp-overview)

**注意（售前必说清）**：

- 功能为 **Public Preview**，**勿对外宣称 GA / 生产已稳**  
- 客户侧需有 **GCP** 项目与计费；**大陆访问与账单**要单独谈清  
- 与本仓库 **Spring AI MCP Apps** 演示是**互补**（存量 REST → Agent 工具），**不是替换**交互面板短包

可作相邻短包或主包附录卖点（例如：对照客户一份 OpenAPI 写若干 `x-google-mcp-tool` 示例说明），范围与价另议。
