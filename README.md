Spring AI MCP Apps Server
===
This example highlight how to use metadata on an MCP server's tools and
resources to enable [MCP Apps](https://modelcontextprotocol.io/extensions/apps/overview).

Building and Running the Server
---
This example is built with Gradle. There is a Gradle wrapper available in the
project directory, so you can build an executable JAR file like this:

```
./gradlew build
```

Then you can run it like this:

```
java -jar build/libs/mcp-apps-server-0.0.1-SNAPSHOT.jar
```

Optionally, you can use the Spring Boot Gradle plugin to build and run the
app like this:

```
./gradlew bootRun
```

Or, if you want, run the application through your IDE using the facilities
of your favorite IDE.

Once the application starts, the MCP server will be listening on port
3001.

Using the MCP App Server
---
To see the MCP App in action, you'll need to configure the server in an
MCP client that supports MCP Apps. This includes MCP Jam and Claude
Desktop. Goose also supports MCP Apps, but does not (yet, as of version
1.27.2) support the ability for the app to update the context model, so
at least for now you cannot use Goose.

When you configure the MCP server, choose Streamable HTTP as the transport
protocol and set the URL to "http://localhost:3001/mcp".

Claude Desktop (at least as of version 1.1.5749) does not support Streamable
HTTP. But you can still use a Streamable HTTP MCP server by using the
mcp-remote (STDIO transport) MCP server to proxy to the Streamable HTTP
server. Configure it in the Claude Desktop configuration JSON like this:

```
"mcpServers": {
  "dice-tools": {
    "command": "npx",
    "args": [
      "-y",
      "mcp-remote",
      "http://localhost:3001/mcp"
    ]
  }
}
```

After configuring the MCP server in your MCP client, type "Roll the dice"
in the chat. If everything is configured correctly, after a moment you
should see a pair of dice, rolled to some values. Then ask "What was the
result?" to verify that the application was about to update the model
context. Then click the "Roll Dice" button to re-roll the dice and ask
again.

Agent audit guardrail (optional demo tools)
---
Alongside the dice MCP App, this repo includes a **minimal write-confirm + audit** demo
(no OAuth by default; does not change the dice UI):

- Read-only tools (`list-account-notes`, `get-demo-balance`, `get-audit-log`) run immediately.
- Mutating tools (`propose-set-account-note`, `propose-demo-transfer`) return a `confirmToken`;
  call `confirm-pending-action` to apply.
- **Amount limits (CNY):** soft threshold **100** → PENDING message flags over-soft; hard limit **10000** → `DENIED` immediately (token cannot bypass).
- Audit: append-only decisions `ALLOWED_READ` / `PENDING` / `APPROVED` / `DENIED`. In-memory ring (max 500) + JSONL file `./data/audit-log.jsonl` (survives restart; demo-grade, not WORM).

Sales appendix (Chinese): `附录-Agent审计护栏短包.md` · outline: `docs/audit-guard-outline.md`.

Agent treasury / send-money human-confirm narrative (same tools; sandbox ledger only—no live Stripe Treasury):
Sales appendix: `附录-Agent资金人审闸短包.md`.

Quick verify after `bootRun` (MCP client or any tools/call): read → propose → confirm → `get-audit-log`.

RCM vertical demo (fictional healthcare coding)
---
Alongside the account audit tools, **GuardedRcmTools** demos a healthcare RCM human-confirm gate
(no PHI, no live insurance / clearinghouse; not clinical advice):

- Read-only: `list-suggested-charges`, `get-claim-draft` → immediate + audit `ALLOWED_READ` (patient label `DEMO-PATIENT`, codes like `DEMO-CHG-100` / `X99.1`).
- Mutating: `propose-apply-charge-codes` → `PENDING` + `confirmToken` only; apply via existing `confirm-pending-action`.
- Limits (CNY / count): soft amount **500** / soft code count **5** → PENDING cue; hard amount **10000** / hard code count **20** → `DENIED`.
- Reuses shared `ConfirmationGate` + `AuditLogStore` JSONL; does not change the Dice MCP Apps UI.

Sales appendix: `附录-医疗RCM人机复核短包.md` · outline: `docs/rcm-guard-outline.md`.
Status: public demo includes RCM vertical (fictional data only; no PHI).


