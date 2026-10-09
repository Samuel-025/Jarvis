package com.example.core.safety

import com.example.core.model.JarvisIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ConfirmationRequest(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val intent: JarvisIntent,
    val timestamp: Long = System.currentTimeMillis()
)

object ConfirmationManager {
    private val _pendingRequest = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingRequest: StateFlow<ConfirmationRequest?> = _pendingRequest.asStateFlow()

    fun requestConfirmation(prompt: String, intent: JarvisIntent): ConfirmationRequest {
        val request = ConfirmationRequest(prompt = prompt, intent = intent)
        _pendingRequest.value = request
        return request
    }

    fun approve(): JarvisIntent? {
        val req = _pendingRequest.value
        _pendingRequest.value = null
        return req?.intent
    }

    fun reject() {
        _pendingRequest.value = null
    }

    fun hasPending(): Boolean = _pendingRequest.value != null
}
