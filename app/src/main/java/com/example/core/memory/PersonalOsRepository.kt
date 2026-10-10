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
        memoryDao.insertMemory(MemoryEntity(key = key, value = value, category = category))
    suspend fun searchMemories(query: String) = memoryDao.searchMemories(query)
    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemory(id)
    suspend fun clearMemories() = memoryDao.clearAllMemories()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    suspend fun saveNote(title: String, content: String) = noteDao.insertNote(NoteEntity(title = title, content = content))
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

    /** Returns a portable OKF v0.2 bundle as relative Markdown paths and file contents. */
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
