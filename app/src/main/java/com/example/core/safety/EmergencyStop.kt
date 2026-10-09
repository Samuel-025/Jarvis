package com.example.core.safety

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EmergencyStop {
    private val _isStopped = MutableStateFlow(false)
    val isStopped: StateFlow<Boolean> = _isStopped.asStateFlow()

    fun trigger(reason: String = "User requested emergency stop") {
        _isStopped.value = true
    }

    fun reset() {
        _isStopped.value = false
    }

    fun checkOrThrow() {
        if (_isStopped.value) {
            throw EmergencyStopException("Emergency stop active. All operations halted.")
        }
    }

    fun isActive(): Boolean = _isStopped.value
}

class EmergencyStopException(message: String) : IllegalStateException(message)
