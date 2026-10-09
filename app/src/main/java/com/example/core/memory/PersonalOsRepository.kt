package com.example.core.memory

import kotlinx.coroutines.flow.Flow

class PersonalOsRepository(
    private val memoryDao: MemoryDao,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val routineDao: RoutineDao
) {
    // Memories
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    suspend fun saveMemory(key: String, value: String, category: String = "general") =
        memoryDao.insertMemory(MemoryEntity(key = key, value = value, category = category))
    suspend fun searchMemories(query: String) = memoryDao.searchMemories(query)
    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemory(id)
    suspend fun clearMemories() = memoryDao.clearAllMemories()

    // Notes
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    suspend fun saveNote(title: String, content: String) =
        noteDao.insertNote(NoteEntity(title = title, content = content))
    suspend fun deleteNote(id: Long) = noteDao.deleteNote(id)
    suspend fun clearNotes() = noteDao.clearAllNotes()

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    suspend fun addTask(title: String, category: String = "General") =
        taskDao.insertTask(TaskEntity(title = title, category = category))
    suspend fun toggleTask(task: TaskEntity) =
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    suspend fun deleteTask(id: Long) = taskDao.deleteTask(id)
    suspend fun clearTasks() = taskDao.clearAllTasks()

    // Routines
    val allRoutines: Flow<List<RoutineEntity>> = routineDao.getAllRoutines()
    suspend fun addRoutine(name: String, triggerPhrase: String, actionsJson: String) =
        routineDao.insertRoutine(RoutineEntity(name = name, triggerPhrase = triggerPhrase, actionsJson = actionsJson))
    suspend fun deleteRoutine(id: Long) = routineDao.deleteRoutine(id)
}
