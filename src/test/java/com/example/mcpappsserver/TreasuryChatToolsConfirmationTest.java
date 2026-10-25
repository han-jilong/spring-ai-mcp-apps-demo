package com.example.mcpappsserver;

import com.example.mcpappsserver.audit.AuditLogStore;
import com.example.mcpappsserver.audit.ConfirmationGate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.model.tool.ToolCallConfirmationPolicy;
import org.springframework.ai.model.tool.ToolCallDecision;
import org.springframework.ai.model.tool.ToolCallDecisions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Official Spring AI 2.1 Tool Call Confirmation alignment — no live LLM.
 * Verifies {@code @Tool(requiresConfirmation)} metadata + policy + sandbox apply path.
 */
class TreasuryChatToolsConfirmationTest {

  @TempDir
  Path tmp;

  GuardedAccountTools accountTools;
  TreasuryChatTools chatTools;

  @BeforeEach
  void setUp() throws Exception {
    AuditLogStore store = newStore(tmp.resolve("audit-log.jsonl"));
    ConfirmationGate gate = new ConfirmationGate();
    accountTools = new GuardedAccountTools(store, gate);
    chatTools = new TreasuryChatTools(accountTools, store);
  }

  @Test
  void mutatingToolsRequireConfirmationReadOnlyDoNot() {
    ToolCallback[] callbacks = ToolCallbacks.from(chatTools);
    Map<String, Boolean> flags = Arrays.stream(callbacks).collect(Collectors.toMap(
        cb -> cb.getToolDefinition().name(),
        cb -> {
          ToolMetadata meta = cb.getToolMetadata();
          return meta != null && meta.requiresConfirmation();
        }));

    assertTrue(flags.getOrDefault("demoTransfer", false), "demoTransfer should require confirmation");
    assertTrue(flags.getOrDefault("setAccountNote", false), "setAccountNote should require confirmation");
    assertFalse(flags.getOrDefault("getDemoBalance", true), "getDemoBalance should be ungated");
    assertFalse(flags.getOrDefault("listAccountNotes", true), "listAccountNotes should be ungated");
    assertFalse(flags.getOrDefault("getAuditLog", true), "getAuditLog should be ungated");
  }

  @Test
  void defaultPolicyMatchesRequiresConfirmationMetadata() {
    ToolCallback[] callbacks = ToolCallbacks.from(chatTools);
    ToolCallConfirmationPolicy policy = ToolCallConfirmationPolicy.fromToolMetadata();

    Map<String, ToolCallback> byName = Arrays.stream(callbacks)
        .collect(Collectors.toMap(cb -> cb.getToolDefinition().name(), cb -> cb));

    assertTrue(needsConfirm(policy, byName.get("demoTransfer")));
    assertTrue(needsConfirm(policy, byName.get("setAccountNote")));
    assertFalse(needsConfirm(policy, byName.get("getDemoBalance")));

    // Property-style name list still works for tools you cannot annotate
    ToolCallConfirmationPolicy named = ToolCallConfirmationPolicy.forTools("demoTransfer");
    AssistantMessage.ToolCall xfer = new AssistantMessage.ToolCall("c1", "function", "demoTransfer", "{}");
    AssistantMessage.ToolCall read = new AssistantMessage.ToolCall("c2", "function", "getDemoBalance", "{}");
    assertTrue(named.requiresConfirmation(xfer, null));
    assertFalse(named.requiresConfirmation(read, null));
  }

  @Test
  void toolCallDecisionsAllowDenyApiSmoke() {
    ToolCallDecisions decisions = ToolCallDecisions.of(
        ToolCallDecision.allow("call_1"),
        ToolCallDecision.deny("call_2", "amount over soft threshold — ask human"));
    assertEquals(2, decisions.decisions().size());
    assertTrue(decisions.decisions().get(0).allowed());
    assertFalse(decisions.decisions().get(1).allowed());
  }

  @Test
  void allowedChatToolMutatesSandboxHardLimitStillDenied() {
    String soft = chatTools.demoTransfer("carol", "150");
    assertTrue(soft.startsWith("APPLIED:"), soft);

    String hard = chatTools.demoTransfer("dave", "10000.01");
    assertTrue(hard.startsWith("DENIED:"), hard);
    assertTrue(hard.contains("hard limit"), hard);

    String bal = chatTools.getDemoBalance();
    assertTrue(bal.contains("4850"), bal); // 5000 - 150
  }

  private static boolean needsConfirm(ToolCallConfirmationPolicy policy, ToolCallback cb) {
    AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
        "call_test", "function", cb.getToolDefinition().name(), "{}");
    return policy.requiresConfirmation(toolCall, cb.getToolMetadata());
  }

  private static AuditLogStore newStore(Path file) throws Exception {
    Constructor<AuditLogStore> ctor = AuditLogStore.class.getDeclaredConstructor(Path.class);
    ctor.setAccessible(true);
    return ctor.newInstance(file);
  }
}
