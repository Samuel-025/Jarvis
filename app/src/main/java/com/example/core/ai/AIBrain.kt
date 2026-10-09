package com.example.core.ai

import com.example.core.model.PrivacyMode

sealed class AIResponse {
    data class Success(val text: String, val tokensUsed: Int = 0) : AIResponse()
    data class Error(val message: String, val isOffline: Boolean = false) : AIResponse()
}

interface AIBrain {
    suspend fun query(prompt: String, privacyMode: PrivacyMode, imageBase64: String? = null): AIResponse
}
