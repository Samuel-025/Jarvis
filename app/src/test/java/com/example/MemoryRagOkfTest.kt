package com.example

import com.example.core.memory.ConversationMessageEntity
import com.example.core.memory.MemoryEntity
import com.example.core.memory.NoteEntity
import com.example.core.memory.OkfMemoryCodec
import com.example.core.memory.RagContextBuilder
import org.junit.Assert.assertEquals
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

    @Test fun ragWeightsTitleMatchMoreThanBodyOnlyMatch() {
        val result = RagContextBuilder.build("Python learning plan", listOf(
            RagContextBuilder.Snippet("Python learning plan", "Review this later", "note", 1),
            RagContextBuilder.Snippet("General note", "Python learning plan was mentioned once", "note", 2)
        ), maxItems = 1)
        assertTrue(result.contains("title=Python learning plan"))
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

    @Test fun okfImportValidatesVersionAndParsesMemoryAndNoteConcepts() {
        val files = mapOf(
            "index.md" to "---\ntype: Knowledge Bundle\nokf_version: \"0.2\"\n---\n",
            "memories/memory-7.md" to "---\ntype: 'Memory'\ntitle: 'Laptop budget'\ndescription: 'preference'\n---\n45000 INR",
            "notes/note-2.md" to "---\ntype: 'Note'\ntitle: 'Study plan'\ndescription: 'Saved note'\n---\nLearn Python",
            "conversations/message-3.md" to "---\ntype: 'Conversation Message'\ntitle: 'USER: hello'\n---\nhello",
            "../unsafe.md" to "ignore"
        )
        val report = OkfMemoryCodec.parseImportBundle(files)
        assertEquals(2, report.items.size)
        assertEquals(2, report.skippedFiles)
        assertTrue(report.errors.isEmpty())
        assertEquals("45000 INR", report.items.first().body)
    }

    @Test fun okfImportRejectsWrongVersionAndMalformedConcepts() {
        val wrongVersion = OkfMemoryCodec.parseImportBundle(mapOf("index.md" to "---\nokf_version: \"9.0\"\n---"))
        assertTrue(wrongVersion.items.isEmpty())
        assertTrue(wrongVersion.errors.isNotEmpty())
        val malformed = OkfMemoryCodec.parseImportBundle(mapOf(
            "index.md" to "---\nokf_version: \"0.2\"\n---",
            "memories/memory-1.md" to "not frontmatter"
        ))
        assertTrue(malformed.items.isEmpty())
        assertTrue(malformed.errors.any { it.contains("frontmatter") })
    }

    @Test fun okfExportImportRoundTripPreservesMemoryAndNoteContent() {
        val exported = OkfMemoryCodec.exportBundle(
            memories = listOf(MemoryEntity(id = 12, key = "Study goal", value = "Finish Python roadmap", category = "learning", timestamp = 100)),
            notes = listOf(NoteEntity(id = 9, title = "Project note", content = "Use local-first memory", timestamp = 101)),
            messages = emptyList()
        )
        val imported = OkfMemoryCodec.parseImportBundle(exported)
        assertEquals(2, imported.items.size)
        assertTrue(imported.items.any { it.title == "Study goal" && it.body == "Finish Python roadmap" })
        assertTrue(imported.items.any { it.title == "Project note" && it.body == "Use local-first memory" })
    }

    @Test fun okfYamlEscapesQuotesInUserSuppliedTitles() {
        val bundle = OkfMemoryCodec.exportBundle(
            memories = listOf(MemoryEntity(id = 1, key = "Suyash's preference", value = "Don't forget", timestamp = 1)),
            notes = emptyList(), messages = emptyList()
        )
        assertTrue(bundle["memories/memory-1.md"]!!.contains("Suyash''s preference"))
    }
}
