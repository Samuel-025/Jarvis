package com.example.core.memory

import java.time.Instant

/** Portable OKF v0.2 knowledge bundle. Each map key is a relative Markdown file path. */
object OkfMemoryCodec {
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
