# 附录 · 可选加购：MCP OAuth / 鉴权短包

> **可选 / 加购**，不改动主包「MCP Apps 交互面板短包」的固定价与边界（¥2.5–6k / 3–7 工作日）。  
> 与同目录 `附录-鉴权与GCP-MCP.md`（A 节轻量鉴权/网关）互补：本附录是**可单独挂单的 OAuth + 按 tool 授权**交付说明。  
> **里程碑栈**（Spring AI 2.0.x / Boot 4.x）——**勿对外宣称 GA / 生产已稳**。  
> 报价供挂单参考，**非保证成交**；起草阶段**无需海外手机号 / 无需付费 API key / 无需替客户办身份证**。

---

## 1. 为什么卖（一句话）

官方写明：HTTP 类 MCP（SSE / Streamable-HTTP / Stateless）**默认无鉴权**——公网裸暴露 `/mcp` 等于谁都能 `tools/list` 和 `tools/call`。  
本加购把「能跑的面板 Demo」升级成「可演示的受保护 MCP」：Bearer/JWT、每 tool 一个 scope、可选网关挡在 `/mcp` 前。

依据：

- [Spring AI · MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)（Securing the MCP Server：默认无鉴权，须自加安全边界）
- 社区教程（2026-10-18）：[Build an MCP Server in Java with Spring AI (+ OAuth)](https://dev.to/jean_claude_van_debug/build-an-mcp-server-in-java-with-spring-ai-oauth-4nb6)（Streamable HTTP + Resource Server + `@PreAuthorize` 按 tool scope + 参数校验）

插接位置草稿（不改坏现有无 key Demo）：同目录 `docs/oauth-outline.md`。

---

## 2. 包名 / 挂单标题

**中文**：MCP OAuth 鉴权加固短包（可挂在 MCP Apps 主包旁）

**English（Upwork / Fiverr）**：  
`Spring AI MCP: OAuth2 Resource Server + Per-Tool Scopes (Streamable HTTP)`

---

## 3. 客户得到什么（固定边界）

| 交付物 | 说明 |
|--------|------|
| **受保护 `/mcp` 演示** | 匿名请求 → **401**（可带 Protected Resource Metadata 指向说明）；带合法 Bearer → `initialize` / `tools/list` 正常 |
| **OAuth2 Resource Server 配置** | JWT `issuer-uri`（或客户现有 IdP）+ 本机可跑的最小授权方说明；**默认路径用本地/客户已有 IdP，不强迫注册海外 SaaS** |
| **按 tool 的 scope** | 至少 2 档（例：`mcp.read` / `mcp.write`）；写操作 tool 配 `@PreAuthorize("hasAuthority('SCOPE_…')")`（或等价） |
| **网关 / 入口一页** | 可选：客户已有 API 网关挡在 `/mcp` 前（鉴权头透传要点）；或仅应用内 Spring Security |
| **参数校验要点** | 对模型传入的 tool 参数按「互联网输入」做长度/格式拒绝示例（防幻觉/注入式乱填） |
| **联调清单 + 30–45 分钟讲解** | curl/客户端如何取 token、带 `Authorization` 头调 `/mcp`；与主包面板如何共存 |

本加购**不替换**主包的交互面板交付；可单独卖，也可与主包合并报价。

---

## 4. 不含什么（防包月）

- ❌ 包月运维、7×24、线上 SLA、渗透测试全包  
- ❌ 替客户采购/代付 Claude、OpenAI、GCP、海外 IdP 订阅  
- ❌ 完整企业 SSO 落地（多租户、SCIM、生产密钥托管）——可另议二期  
- ❌ 保证各 MCP 客户端对 OAuth 发现流程的长期兼容（协议与客户端仍在演进）  
- ❌ 代办大陆/海外实名认证、手机号、支付账号

---

## 5. 参考报价（加购）

**¥2,000 – ¥5,000**（人民币，固定价 · **3–7 个工作日**）

| 档位 | 典型内容 |
|------|----------|
| 偏下限 | API Key / 单一 Bearer + `/mcp` 保护 + 配置清单 + 讲解 |
| 中档 | JWT Resource Server + 2 档 per-tool scope + 本地/客户 IdP 联调说明 |
| 接近上限 | 上项 + 客户网关规则草稿 + 参数校验示例 + 与主包面板合并演示 |

与主包合并时：主包仍按 ¥2.5–6k；本加购**另计**，书面范围分开写清。  
轻量「只挡一层」也可走 `附录-鉴权与GCP-MCP.md` A 节另议，不必上本 OAuth 满配。

---

## 6. 交付节奏（示例）

- **D1**：对齐鉴权方式（API Key vs JWT）、scope 清单、是否有现成 IdP/网关  
- **D2–4**：Resource Server / Security 配置、按 tool 授权、401/合法 token 联调  
- **D5–7**：校验示例、文档收尾、远程讲解  

收到「用哪种 token、哪几个 tool 要写权限」后起算。

---

## 7. 大陆友好 / 零前期花费（售前必说）

| 项 | 口径 |
|----|------|
| **起草与本地 Demo** | 可用本机 Spring Authorization Server（或客户内网 IdP）+ `client_credentials`；**不要求**海外手机号、护照、付费 LLM key |
| **客户生产 IdP** | 优先对接客户**已有**统一登录 / 内网 OIDC；海外 SaaS 仅当客户自有账号且自付 |
| **主包无 key 起服** | 本仓库现有 Demo **仍可无 key 起服**；OAuth 为**可选 profile / 加购交付**，默认不强制打开以免打断主包验证路径 |
| **里程碑** | Spring AI 2.x + 社区 MCP Security starter 等均为演进中组件——对外只说「可演示、短包交付」，不说「GA 生产无忧」 |

---

## 8. 技术挂点（摘要）

详见 `docs/oauth-outline.md`。要点：

1. 依赖侧（加购时再加，**勿默认写进主包 build**）：OAuth2 Resource Server；可选社区 `mcp-server-security-spring-boot`（版本以交付当日文档为准）。  
2. 配置：`spring.security.oauth2.resourceserver.jwt.issuer-uri=…`；MCP 仍用 `spring.ai.mcp.server.protocol=streamable`（本仓库已设）。  
3. 代码：`@EnableMethodSecurity` + 各 `@McpTool` 上 `@PreAuthorize`；可选 `TransportContextExtractor` 把 `Authorization` 头放进 `McpTransportContext`（见官方 Boot Starter 文档）。  
4. 入口：应用内 Security，或网关先验 token 再反代 `localhost:3001/mcp`。

---

## 9. 一句话话术（给接单助手）

官方 HTTP MCP **默认无鉴权**；本加购用 Spring 把 Streamable `/mcp` 做成 OAuth2 资源服务器，**每个 tool 独立 scope**，可选网关挡入口，**3–7 天、参考 ¥2–5K**（里程碑栈，可与 MCP Apps 面板主包搭售；非保证成交；本地可演示，不强迫海外账号）。

---

## 10. 相关链接

- 官方文档：[MCP Server Boot Starter · Securing](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)  
- 教程：[Spring AI MCP + OAuth（dev.to，2026-10-18）](https://dev.to/jean_claude_van_debug/build-an-mcp-server-in-java-with-spring-ai-oauth-4nb6)  
- 主包：`报价包.md` · 轻量鉴权/GCP：`附录-鉴权与GCP-MCP.md` · 插接草稿：`docs/oauth-outline.md`
