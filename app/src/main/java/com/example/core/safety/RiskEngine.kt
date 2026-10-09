package com.example.core.safety

import com.example.core.model.JarvisIntent
import com.example.core.model.RiskAssessment
import com.example.core.model.RiskLevel

object RiskEngine {
    fun assess(intent: JarvisIntent, privacyMode: com.example.core.model.PrivacyMode = com.example.core.model.PrivacyMode.BALANCED): RiskAssessment {
        return when (intent) {
            is JarvisIntent.StopAll -> RiskAssessment(
                level = RiskLevel.LOW,
                requiresConfirmation = false,
                reason = "Emergency stop always permitted immediately"
            )
            is JarvisIntent.ClearAllMemories -> RiskAssessment(
                level = RiskLevel.HIGH,
                requiresConfirmation = true,
                reason = "Clearing all persistent memories is irreversible"
            )
            is JarvisIntent.PhoneAction -> {
                if (intent.actionType.equals("reset", ignoreCase = true) ||
                    intent.actionType.contains("delete", ignoreCase = true)
                ) {
                    RiskAssessment(RiskLevel.HIGH, true, "Destructive phone operation")
                } else {
                    RiskAssessment(RiskLevel.MEDIUM, false, "Standard phone action")
                }
            }
            is JarvisIntent.Flashlight -> RiskAssessment(RiskLevel.LOW, false, "Toggles device torch")
            is JarvisIntent.VolumeControl -> RiskAssessment(RiskLevel.LOW, false, "Adjusts device volume")
            is JarvisIntent.BatteryStatus, is JarvisIntent.DateTimeQuery -> RiskAssessment(RiskLevel.LOW, false, "Read-only device telemetry")
            is JarvisIntent.LaunchApp, is JarvisIntent.OpenSettings -> RiskAssessment(RiskLevel.LOW, false, "Standard intent navigation")
            is JarvisIntent.CreateNote, is JarvisIntent.CreateTask -> RiskAssessment(RiskLevel.LOW, false, "Additive local data creation")
            is JarvisIntent.QueryMemory -> RiskAssessment(RiskLevel.LOW, false, "Read-only local memory query")
            is JarvisIntent.VisionAnalyze -> RiskAssessment(RiskLevel.LOW, false, "Vision inspection")
            is JarvisIntent.AgentPlan -> RiskAssessment(RiskLevel.MEDIUM, true, "Multi-step autonomous agent execution requires confirmation")
            is JarvisIntent.GeneralQuery -> {
                if (privacyMode == com.example.core.model.PrivacyMode.BALANCED) {
                    RiskAssessment(
                        level = RiskLevel.MEDIUM,
                        requiresConfirmation = true,
                        reason = "BALANCED privacy mode requires explicit consent before sending query to cloud AI"
                    )
                } else {
                    RiskAssessment(RiskLevel.LOW, false, "General query under ${privacyMode.name} mode")
                }
            }
            is JarvisIntent.Unknown -> RiskAssessment(RiskLevel.LOW, false, "Unknown intent")
        }
    }
}
