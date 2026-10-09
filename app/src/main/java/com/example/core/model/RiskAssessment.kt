package com.example.core.model

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class RiskAssessment(
    val level: RiskLevel,
    val requiresConfirmation: Boolean,
    val reason: String
)
