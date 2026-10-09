package com.example.core.engine

import com.example.core.model.CommandResult
import com.example.core.model.JarvisIntent

interface LocalCommandEngine {
    suspend fun execute(intent: JarvisIntent): CommandResult
}
