# Box notes (not for customer)

- Upstream `build.gradle` toolchain was Java **25**; this box has Temurin **21** → changed to `JavaLanguageVersion.of(21)`.
- `./gradlew build` SUCCESS; boot JAR listens on **3001**; MCP initialize works **without** any model API key.
- Full in-chat UI needs an MCP Apps–capable client (MCP Jam / Claude Desktop + mcp-remote).
