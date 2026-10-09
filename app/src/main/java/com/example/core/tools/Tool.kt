package com.example.core.tools

import com.example.core.model.CommandResult
import com.example.core.model.JarvisIntent

interface Tool {
    val name: String
    val description: String
    val requiredPermission: String? get() = null
    fun canHandle(intent: JarvisIntent): Boolean
    suspend fun execute(intent: JarvisIntent): CommandResult
}

class ToolRegistry {
    private val tools = mutableListOf<Tool>()

    fun register(tool: Tool) {
        tools.add(tool)
    }

    fun getAll(): List<Tool> = tools.toList()

    fun findHandler(intent: JarvisIntent): Tool? {
        return tools.firstOrNull { it.canHandle(intent) }
    }
}
