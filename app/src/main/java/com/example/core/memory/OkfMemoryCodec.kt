package com.example.core.memory

import java.time.Instant

/** Portable OKF v0.2 knowledge bundle. Each map key is a relative Markdown file path. */
object OkfMemoryCodec {
    data class ImportItem(val kind: Kind, val title: String, val body: String, val category: String = "imported") {
        enum class Kind { MEMORY, NOTE }
    }

    data class ImportReport(val items: List<ImportItem>, val errors: List<String>, val skippedFiles: Int)

    fun exportBundle(memories: List<MemoryEntity>, notes: List<NoteEntity>, messages: List<ConversationMessageEntity>): Map<String, String> {
        val bundle = linkedMapOf<String, String>()
        bundle["index.md"] = """---
type: Knowledge Bundle
title: Jarvis Mobile Memory
description: Portable export of user-approved Jarvis memories, notes, and conversation history.
okf_version: "0.2"
generated:
  by: jarvis-mobile/5.5
  at: "${Instant.now()}"
tags: [jarvis, personal-memory, rag]
---

# Jarvis Mobile Memory

This bundle contains exported memories, notes, and conversation messages. Keep this private: it may contain personal information.
""".trimIndent()
        memories.forEach { item ->
            bundle["memories/memory-${item.id}.md"] = concept("Memory", item.key, item.category, item.timestamp, item.value, listOf("memory", item.category))
        }
        notes.forEach { item ->
            bundle["notes/note-${item.id}.md"] = concept("Note", item.title, "Saved Jarvis note", item.timestamp, item.content, listOf("note", "jarvis"))
        }
        messages.forEach { item ->
            bundle["conversations/message-${item.id}.md"] = concept("Conversation Message", "${item.role}: ${item.content.take(72)}", "Persisted conversation message from ${item.source}", item.timestamp, item.content, listOf("conversation", item.role.lowercase(), "jarvis"))
        }
        return bundle
    }

    /** Validate a decoded OKF bundle. Only memory/note concepts are importable; chat logs are not trusted memory. */
    fun parseImportBundle(files: Map<String, String>): ImportReport {
        val index = files["index.md"]
            ?: return ImportReport(emptyList(), listOf("Missing index.md"), files.size)
        if (!Regex("""(?m)^okf_version:\s*["']?0\.2["']?\s*$""").containsMatchIn(index)) {
            return ImportReport(emptyList(), listOf("Unsupported or missing OKF version; expected 0.2"), (files.size - 1).coerceAtLeast(0))
        }
        val items = mutableListOf<ImportItem>()
        val errors = mutableListOf<String>()
        var skipped = 0
        files.forEach { (path, text) ->
            if (path == "index.md") return@forEach
            val kind = when {
                Regex("""^memories/memory-[A-Za-z0-9_-]+\.md$""").matches(path) -> ImportItem.Kind.MEMORY
                Regex("""^notes/note-[A-Za-z0-9_-]+\.md$""").matches(path) -> ImportItem.Kind.NOTE
                else -> { skipped++; return@forEach }
            }
            if (text.length > 100_000) {
                errors += "$path: file exceeds 100 KB limit"
                return@forEach
            }
            val parsed = parseConcept(text)
            if (parsed == null) {
                errors += "$path: invalid Markdown frontmatter"
                return@forEach
            }
            val expectedType = if (kind == ImportItem.Kind.MEMORY) "Memory" else "Note"
            if (!parsed.type.equals(expectedType, ignoreCase = true)) {
                errors += "$path: concept type does not match its folder"
                return@forEach
            }
            val title = parsed.title.trim().take(200)
            val body = parsed.body.trim().take(12000)
            if (title.isBlank() || body.isBlank()) {
                errors += "$path: title and body must be non-empty"
                return@forEach
            }
            val category = if (kind == ImportItem.Kind.MEMORY) parsed.category.ifBlank { "imported" }.take(80) else "imported"
            items += ImportItem(kind, title, body, category)
        }
        return ImportReport(items, errors, skipped)
    }

    private data class ParsedConcept(val type: String, val title: String, val body: String, val category: String)

    private fun parseConcept(text: String): ParsedConcept? {
        if (!text.startsWith("---")) return null
        val end = text.indexOf("\n---", startIndex = 3)
        if (end < 0) return null
        val header = text.substring(3, end).trim()
        val bodyStart = text.indexOf('\n', end + 4)
        if (bodyStart < 0) return null
        val body = text.substring(bodyStart + 1).trim()
        fun field(name: String): String {
            val line = header.lineSequence().firstOrNull { it.startsWith("$name:") } ?: return ""
            return unquote(line.substringAfter(':').trim())
        }
        return ParsedConcept(field("type"), field("title"), body, field("description"))
    }

    private fun unquote(value: String): String {
        if (value.length >= 2 && value.first() == '\'' && value.last() == '\'') return value.substring(1, value.length - 1).replace("''", "'")
        if (value.length >= 2 && value.first() == '"' && value.last() == '"') return value.substring(1, value.length - 1).replace("\\\"", "\"")
        return value
    }

    private fun concept(type: String, title: String, description: String, timestamp: Long, body: String, tags: List<String>): String {
        val tagsYaml = tags.distinct().joinToString(", ") { yaml(it) }
        return """---
type: ${yaml(type)}
title: ${yaml(title)}
description: ${yaml(description.take(180))}
tags: [$tagsYaml]
generated:
  by: jarvis-mobile/5.5
  at: "${Instant.ofEpochMilli(timestamp)}"
status: current
---

$body
""".trimIndent()
    }

    private fun yaml(value: String): String = "'" + value.replace("'", "''").replace("\n", " ").replace("\r", " ") + "'"
}
