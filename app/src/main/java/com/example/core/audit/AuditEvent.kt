package com.example.core.audit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AuditEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,       // "USER_VOICE", "USER_TEXT", "AGENT", "SYSTEM"
    val action: String,
    val details: String,
    val success: Boolean,
    val riskLevel: String = "LOW"
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

class AuditEventRepository {
    private val _events = MutableStateFlow<List<AuditEvent>>(emptyList())
    val events: StateFlow<List<AuditEvent>> = _events.asStateFlow()

    fun log(event: AuditEvent) {
        _events.value = listOf(event) + _events.value.take(99) // Keep last 100 in-memory
    }

    fun clear() {
        _events.value = emptyList()
    }
}
