package com.example

import com.example.core.memory.ConversationMessageEntity
import com.example.core.memory.MemoryEntity
import com.example.core.memory.NoteEntity
import com.example.core.memory.OkfMemoryCodec
import com.example.core.memory.RagContextBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRagOkfTest {
    @Test fun ragRanksRelevantSavedFactsAndIgnoresUnrelatedFacts() {
        val snippets = listOf(
            RagContextBuilder.Snippet("Preferred laptop", "The laptop budget is 45000 rupees", "memory", 2),
            RagContextBuilder.Snippet("Recipe", "Masala bhindi is fried in oil first", "note", 3),
            RagContextBuilder.Snippet("Workout", "Beginner calisthenics at home", "conversation", 1)
        )
        val result = RagContextBuilder.build("What was my laptop budget?", snippets)
        assertTrue(result.contains("45000 rupees"))
        assertFalse(result.contains("Masala bhindi"))
    }

    @Test fun ragReturnsEmptyWhenNoRelevantTermsExist() {
        val result = RagContextBuilder.build("space telescope", listOf(
            RagContextBuilder.Snippet("Meal plan", "vegetarian homemade meals", "note", 1)
        ))
        assertTrue(result.isEmpty())
    }

    @Test fun okfBundleContainsV02FrontmatterAndPortableConceptFiles() {
        val bundle = OkfMemoryCodec.exportBundle(
            memories = listOf(MemoryEntity(id = 7, key = "Laptop budget", value = "45000 INR", category = "preference", timestamp = 1_790_000_000_000)),
            notes = listOf(NoteEntity(id = 2, title = "Study plan", content = "Learn Python", timestamp = 1_790_000_000_000)),
            messages = listOf(ConversationMessageEntity(id = 3, role = "USER", content = "Remember my plan", timestamp = 1_790_000_000_000))
        )
        assertTrue(bundle.containsKey("index.md"))
        assertTrue(bundle["index.md"]!!.contains("okf_version:") && bundle["index.md"]!!.contains("0.2"))
        assertTrue(bundle["memories/memory-7.md"]!!.contains("45000 INR"))
        assertTrue(bundle["notes/note-2.md"]!!.contains("Learn Python"))
        assertTrue(bundle["conversations/message-3.md"]!!.contains("Remember my plan"))
    }

    @Test fun okfYamlEscapesQuotesInUserSuppliedTitles() {
        val bundle = OkfMemoryCodec.exportBundle(
            memories = listOf(MemoryEntity(id = 1, key = "Suyash's preference", value = "Don't forget", timestamp = 1)),
            notes = emptyList(), messages = emptyList()
        )
        assertTrue(bundle["memories/memory-1.md"]!!.contains("Suyash''s preference"))
    }
}
