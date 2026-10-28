package com.example.core.tools

import com.example.core.model.CommandResult
import com.example.core.model.JarvisIntent

/**
 * A capability that can execute one or more Jarvis intents.
 * requiredPermission remains for source compatibility; new tools may override requiredPermissions.
 */
interface Tool {
    val name: String
    val description: String
    val requiredPermission: String? get() = null
    val requiredPermissions: Set<String>
        get() = requiredPermission?.let(::setOf) ?: emptySet()
    val id: String get() = name.trim().lowercase()

    fun canHandle(intent: JarvisIntent): Boolean
    suspend fun execute(intent: JarvisIntent): CommandResult
}

/** Thread-safe deterministic registry. Duplicate IDs are rejected, registration order is retained. */
class ToolRegistry {
    private val lock = Any()
    private val toolsById = linkedMapOf<String, Tool>()

    /** Returns true when registered; false for a blank or duplicate ID. */
    fun register(tool: Tool): Boolean = synchronized(lock) {
        val key = tool.id.trim().lowercase()
        if (key.isBlank() || toolsById.containsKey(key)) false
        else {
            toolsById[key] = tool
            true
        }
    }

    fun getAll(): List<Tool> = synchronized(lock) { toolsById.values.toList() }

    fun getById(id: String): Tool? = synchronized(lock) {
        toolsById[id.trim().lowercase()]
    }

    fun findHandler(intent: JarvisIntent): Tool? = synchronized(lock) {
        toolsById.values.firstOrNull { it.canHandle(intent) }
    }

    fun findHandlers(intent: JarvisIntent): List<Tool> = synchronized(lock) {
        toolsById.values.filter { it.canHandle(intent) }
    }

    fun unregister(id: String): Boolean = synchronized(lock) {
        toolsById.remove(id.trim().lowercase()) != null
    }

    fun clear() = synchronized(lock) { toolsById.clear() }
}
