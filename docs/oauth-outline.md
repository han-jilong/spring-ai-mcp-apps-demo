# OAuth / Security 插接大纲（可选 · 不加购不启用）

> 目的：标出「鉴权加购」时 Security / OAuth 该挂在哪。  
> **本文件不改变默认运行路径**：主包仍 `./gradlew build` + jar 无 key 起服。  
> 真正加依赖与 `SecurityFilterChain` 仅在客户确认加购后进行，避免打断已验证的无 key Demo。

---

## 现状（主包）

| 文件 | 现状 |
|------|------|
| `build.gradle` | 仅 `spring-boot-starter-webmvc` + `spring-ai-starter-mcp-server-webmvc`；**无** Security / OAuth |
| `application.properties` | `protocol=streamable`，`server.port=3001` |
| `McpAppsServerApplication.java` | 空壳 `@SpringBootApplication` |
| `DiceApp.java` | `@McpTool` / `@McpResource`，无 `@PreAuthorize` |

官方提醒：HTTP MCP 端点默认无鉴权，见  
https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html

---

## 加购时建议挂点（伪配置 · 勿直接粘贴即当生产）

### 1) 依赖（仅加购分支 / profile）

```gradle
// OPTIONAL — enable only for OAuth add-on delivery
// implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
// implementation 'org.springframework.boot:spring-boot-starter-security'
// optional community helper (pin version at delivery time):
// implementation 'org.springaicommunity:mcp-server-security-spring-boot:…'
```

### 2) `application.properties`（或 `application-oauth.yml`）

```properties
# --- OPTIONAL oauth profile (do not enable by default) ---
# spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000
# # Prefer customer's existing OIDC issuer in real projects.
# # Local Spring Authorization Server is fine for mainland-friendly demos
# # (no overseas phone / paid LLM key required to draft).
```

建议用 **Spring profile `oauth`**：默认 profile 保持今日无 key 行为；演示鉴权时再 `--spring.profiles.active=oauth`。

### 3) Security 配置类（新建，加购时）

```text
@EnableMethodSecurity
SecurityFilterChain:
  - permit health/actuator if any
  - anyRequest to /mcp (and MCP resource paths) → authenticated (JWT Bearer)
  - anonymous → 401 + WWW-Authenticate (resource metadata if using MCP OAuth discovery)
```

参考教程步骤：  
https://dev.to/jean_claude_van_debug/build-an-mcp-server-in-java-with-spring-ai-oauth-4nb6

### 4) 按 tool 授权（改 `DiceApp` 或客户面板类）

```text
@PreAuthorize("hasAuthority('SCOPE_mcp.read')")   // 只读 tool / resource
@PreAuthorize("hasAuthority('SCOPE_mcp.write')")  // 会改状态的 tool
```

Scope 名称与客户 IdP 对齐；至少演示「只读 token 调写操作 → Access Denied」。

### 5) 可选：把头放进 MCP 上下文

官方 Boot Starter 文档中的 `TransportContextExtractor`：从 HTTP `Authorization` 写入 `McpTransportContext`，供 tool 内读取（仍须先过 Security，**不能**只靠 tool 内自检代替入口鉴权）。

### 6) 可选：网关挡在 `/mcp` 前

```text
Client → API Gateway (验 token / API Key) → http://mcp-host:3001/mcp
```

应用内可仍保留 Resource Server 作第二道；短包通常二选一写进范围，避免双份运维。

---

## 本地联调检查单（加购交付用）

1. 无 token：`POST /mcp` initialize → **401**  
2. 只读 token：能 `tools/list` / 只读 tool；写 tool → 拒绝  
3. 读写 token：写 tool 成功  
4. 主包面板 HTML 资源在鉴权开启后仍可按约定策略访问（范围里写清：同 token / 或网关放行静态 ui）  
5. 默认 profile：**不启 oauth** 时，原无 key Demo 行为不变  

---

## 明确不做（本大纲）

- 不在主分支默认打开 Security（会破坏「无 key 起服」售前验证）  
- 不提交真实 client-secret / 生产 issuer  
- 不要求交付方或客户为起草阶段购买海外 IdP、手机号验证或 LLM key  
