package com.example.mcpappsserver.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Append-only audit store. Keeps a bounded in-memory ring for fast
 * {@link #recent(int)} and mirrors every row to a JSONL file so restart
 * keeps history (demo-grade, not WORM/SIEM).
 */
@Component
public class AuditLogStore {

  /** Soft cap on in-memory rows; older rows remain on disk only. */
  public static final int MAX_IN_MEMORY = 500;

  private final Path filePath;
  private final ConcurrentLinkedDeque<AuditEntry> entries = new ConcurrentLinkedDeque<>();

  @Autowired
  public AuditLogStore(
      @Value("${audit.log.file:./data/audit-log.jsonl}") String filePath) {
    this.filePath = Path.of(filePath).toAbsolutePath().normalize();
    loadFromDisk();
  }

  /** Package-visible for unit tests with a temp file. */
  AuditLogStore(Path filePath) {
    this.filePath = filePath.toAbsolutePath().normalize();
    loadFromDisk();
  }

  public Path filePath() {
    return filePath;
  }

  public AuditEntry append(String actor, String toolName, String argsSummary,
      String decision, String detail) {
    AuditEntry entry = new AuditEntry(
        UUID.randomUUID().toString().substring(0, 8),
        Instant.now(),
        actor == null || actor.isBlank() ? "agent" : actor,
        toolName,
        argsSummary == null ? "" : argsSummary,
        decision,
        detail == null ? "" : detail);
    entries.addFirst(entry);
    while (entries.size() > MAX_IN_MEMORY) {
      entries.removeLast();
    }
    persist(entry);
    return entry;
  }

  public List<AuditEntry> recent(int limit) {
    int n = Math.max(1, Math.min(limit, 100));
    List<AuditEntry> out = new ArrayList<>(n);
    for (AuditEntry e : entries) {
      out.add(e);
      if (out.size() >= n) {
        break;
      }
    }
    return out;
  }

  public int size() {
    return entries.size();
  }

  private void loadFromDisk() {
    if (!Files.isRegularFile(filePath)) {
      return;
    }
    try {
      List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
      List<AuditEntry> loaded = new ArrayList<>();
      for (String line : lines) {
        if (line == null || line.isBlank()) {
          continue;
        }
        AuditEntry e = parseJsonl(line.trim());
        if (e != null) {
          loaded.add(e);
        }
      }
      // File is chronological (oldest→newest); memory wants newest-first.
      int from = Math.max(0, loaded.size() - MAX_IN_MEMORY);
      for (int i = loaded.size() - 1; i >= from; i--) {
        entries.addLast(loaded.get(i));
      }
    }
    catch (IOException ignored) {
      // Demo: start empty if corrupt/unreadable
    }
  }

  private void persist(AuditEntry entry) {
    try {
      Path parent = filePath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      try (BufferedWriter w = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
        w.write(toJsonl(entry));
        w.newLine();
      }
    }
    catch (IOException ignored) {
      // Demo: memory still holds the row
    }
  }

  static String toJsonl(AuditEntry e) {
    return "{"
        + "\"id\":\"" + esc(e.id()) + "\","
        + "\"timestamp\":\"" + e.timestamp() + "\","
        + "\"actor\":\"" + esc(e.actor()) + "\","
        + "\"toolName\":\"" + esc(e.toolName()) + "\","
        + "\"argsSummary\":\"" + esc(e.argsSummary()) + "\","
        + "\"decision\":\"" + esc(e.decision()) + "\","
        + "\"detail\":\"" + esc(e.detail()) + "\""
        + "}";
  }

  static AuditEntry parseJsonl(String line) {
    try {
      String id = field(line, "id");
      String ts = field(line, "timestamp");
      String actor = field(line, "actor");
      String tool = field(line, "toolName");
      String args = field(line, "argsSummary");
      String decision = field(line, "decision");
      String detail = field(line, "detail");
      if (id == null || ts == null || decision == null) {
        return null;
      }
      return new AuditEntry(id, Instant.parse(ts),
          actor == null ? "agent" : actor,
          tool == null ? "" : tool,
          args == null ? "" : args,
          decision,
          detail == null ? "" : detail);
    }
    catch (Exception ex) {
      return null;
    }
  }

  /** Minimal JSON string field extractor for our flat demo rows. */
  private static String field(String json, String key) {
    String needle = "\"" + key + "\":\"";
    int i = json.indexOf(needle);
    if (i < 0) {
      return null;
    }
    int start = i + needle.length();
    StringBuilder sb = new StringBuilder();
    for (int p = start; p < json.length(); p++) {
      char c = json.charAt(p);
      if (c == '\\' && p + 1 < json.length()) {
        char n = json.charAt(++p);
        sb.append(switch (n) {
          case 'n' -> '\n';
          case 'r' -> '\r';
          case 't' -> '\t';
          case '"' -> '"';
          case '\\' -> '\\';
          default -> n;
        });
        continue;
      }
      if (c == '"') {
        break;
      }
      sb.append(c);
    }
    return sb.toString();
  }

  private static String esc(String s) {
    if (s == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(s.length() + 8);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\' -> sb.append("\\\\");
        case '"' -> sb.append("\\\"");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }
}
