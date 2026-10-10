package com.example.core.memory

import kotlinx.coroutines.flow.Flow

class PersonalOsRepository(
    private val memoryDao: MemoryDao,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val routineDao: RoutineDao,
    private val conversationDao: ConversationDao
) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allRoutines: Flow<List<RoutineEntity>> = routineDao.getAllRoutines()
    val recentConversation: Flow<List<ConversationMessageEntity>> = conversationDao.getRecentMessages(80)

    suspend fun saveMemory(key: String, value: String, category: String = "general") =
        memoryDao.insertMemory(MemoryEntity(key = key, value = redactSecrets(value).trim().take(12000), category = category))
    suspend fun searchMemories(query: String) = memoryDao.searchMemories(query)
    suspend fun updateMemory(memory: MemoryEntity) = memoryDao.updateMemory(
        memory.copy(key = memory.key.trim().take(200), value = redactSecrets(memory.value).trim().take(12000), category = memory.category.trim().take(80))
    )
    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemory(id)
    suspend fun clearMemories() = memoryDao.clearAllMemories()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    suspend fun saveNote(title: String, content: String) = noteDao.insertNote(
        NoteEntity(title = title.trim().take(200), content = redactSecrets(content).trim().take(12000))
    )
    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(
        note.copy(title = note.title.trim().take(200), content = redactSecrets(note.content).trim().take(12000))
    )
    suspend fun deleteNote(id: Long) = noteDao.deleteNote(id)
    suspend fun clearNotes() = noteDao.clearAllNotes()

    suspend fun addTask(title: String, category: String = "General") = taskDao.insertTask(TaskEntity(title = title, category = category))
    suspend fun toggleTask(task: TaskEntity) = taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    suspend fun deleteTask(id: Long) = taskDao.deleteTask(id)
    suspend fun clearTasks() = taskDao.clearAllTasks()

    suspend fun addRoutine(name: String, triggerPhrase: String, actionsJson: String) =
        routineDao.insertRoutine(RoutineEntity(name = name, triggerPhrase = triggerPhrase, actionsJson = actionsJson))
    suspend fun deleteRoutine(id: Long) = routineDao.deleteRoutine(id)

    suspend fun saveConversationMessage(role: String, content: String, source: String, sessionId: String = "main") {
        val clean = redactSecrets(content).trim().take(12000)
        if (clean.isNotBlank()) conversationDao.insert(ConversationMessageEntity(sessionId = sessionId, role = role.uppercase(), content = clean, source = source))
    }

    suspend fun clearConversation() = conversationDao.clearAll()

    /** Retrieve relevant local memories, notes, and prior chat turns without any network call. */
    suspend fun buildRagContext(query: String): String {
        val memories = memoryDao.getRecentMemories(100).map { RagContextBuilder.Snippet(it.key, it.value, "memory/${it.category}", it.timestamp) }
        val notes = noteDao.getRecentNotes(100).map { RagContextBuilder.Snippet(it.title, it.content, "note", it.timestamp) }
        val messages = conversationDao.getRecentMessagesOnce(200)
            .filterNot { it.role.equals("USER", true) && it.content.equals(query.trim(), true) }
            .map { RagContextBuilder.Snippet(it.role, it.content, "conversation/${it.role.lowercase()}", it.timestamp) }
        return RagContextBuilder.build(query, memories + notes + messages)
    }

    data class OkfImportSummary(val importedMemories: Int, val importedNotes: Int, val skippedFiles: Int, val errors: List<String>)

    /**
     * Imports validated OKF concepts without replacing existing rows. Identical title/value pairs
     * are skipped, making repeated imports safe. The caller controls ZIP decoding and user consent.
     */
    suspend fun importOkfBundle(files: Map<String, String>): OkfImportSummary {
        val report = OkfMemoryCodec.parseImportBundle(files)
        var importedMemories = 0
        var importedNotes = 0
        val existingMemories = memoryDao.getAllMemoriesOnce().map { it.key.trim().lowercase() to it.value.trim().lowercase() }.toMutableSet()
        val existingNotes = noteDao.getAllNotesOnce().map { it.title.trim().lowercase() to it.content.trim().lowercase() }.toMutableSet()
        report.items.forEach { item ->
            when (item.kind) {
                OkfMemoryCodec.ImportItem.Kind.MEMORY -> {
                    val key = item.title.trim()
                    val value = redactSecrets(item.body).trim().take(12000)
                    if (existingMemories.add(key.lowercase() to value.lowercase())) {
                        memoryDao.insertMemory(MemoryEntity(key = key.take(200), value = value, category = item.category.take(80)))
                        importedMemories++
                    }
                }
                OkfMemoryCodec.ImportItem.Kind.NOTE -> {
                    val title = item.title.trim()
                    val content = redactSecrets(item.body).trim().take(12000)
                    if (existingNotes.add(title.lowercase() to content.lowercase())) {
                        noteDao.insertNote(NoteEntity(title = title.take(200), content = content))
                        importedNotes++
                    }
                }
            }
        }
        return OkfImportSummary(importedMemories, importedNotes, report.skippedFiles, report.errors)
    }

    private fun redactSecrets(text: String): String = text
        .replace(Regex("(?i)\\bsk-(?:or-v1-)?[A-Za-z0-9_-]{16,}\\b"), "[REDACTED_SECRET]")
        .replace(Regex("\\bAIza[0-9A-Za-z_-]{20,}\\b"), "[REDACTED_SECRET]")
        .replace(Regex("\\bgsk_[A-Za-z0-9_-]{16,}\\b"), "[REDACTED_SECRET]")

    suspend fun exportOkfBundle(): Map<String, String> = OkfMemoryCodec.exportBundle(
        memories = memoryDao.getAllMemoriesOnce(),
        notes = noteDao.getAllNotesOnce(),
        messages = conversationDao.getAllMessagesOnce()
    )
}
