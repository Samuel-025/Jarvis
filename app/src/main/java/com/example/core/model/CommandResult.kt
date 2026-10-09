package com.example.core.model

sealed class CommandResult {
    data class Success(
        val message: String,
        val details: Map<String, String> = emptyMap(),
        val audioFeedback: String? = null
    ) : CommandResult()

    data class RequiresConfirmation(
        val actionId: String,
        val confirmationPrompt: String,
        val pendingIntent: JarvisIntent
    ) : CommandResult()

    data class Error(
        val message: String,
        val errorType: ErrorType = ErrorType.EXECUTION_FAILED,
        val recoverySuggestion: String? = null
    ) : CommandResult()
}

enum class ErrorType {
    PERMISSION_DENIED,
    HARDWARE_UNAVAILABLE,
    APP_NOT_FOUND,
    EMERGENCY_STOPPED,
    PRIVACY_RESTRICTED,
    TIMEOUT,
    EXECUTION_FAILED,
    INVALID_INPUT
}
