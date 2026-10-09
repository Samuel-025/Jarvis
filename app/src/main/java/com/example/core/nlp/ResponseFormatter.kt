package com.example.core.nlp

import com.example.core.model.CommandResult
import com.example.core.model.JarvisIntent

object ResponseFormatter {
    fun format(result: CommandResult): String {
        return when (result) {
            is CommandResult.Success -> {
                result.audioFeedback ?: result.message
            }
            is CommandResult.RequiresConfirmation -> {
                "Confirmation required: ${result.confirmationPrompt}"
            }
            is CommandResult.Error -> {
                val suggestion = result.recoverySuggestion?.let { " ($it)" } ?: ""
                "Action could not be completed: ${result.message}$suggestion"
            }
        }
    }

    fun formatIntentDescription(intent: JarvisIntent): String {
        return when (intent) {
            is JarvisIntent.LaunchApp -> "Launch application '${intent.appQuery}'"
            is JarvisIntent.OpenSettings -> "Open Android settings for '${intent.settingType}'"
            is JarvisIntent.Flashlight -> if (intent.enable) "Turn flashlight ON" else "Turn flashlight OFF"
            is JarvisIntent.VolumeControl -> "Adjust device volume: ${intent.action} ${intent.levelPercent?.let { "to $it%" } ?: ""}"
            is JarvisIntent.BatteryStatus -> "Query device battery status"
            is JarvisIntent.DateTimeQuery -> "Query current date and time"
            is JarvisIntent.StopAll -> "EMERGENCY STOP all actions"
            is JarvisIntent.PhoneAction -> "Execute phone action '${intent.actionType}'"
            is JarvisIntent.VisionAnalyze -> "Analyze image with prompt '${intent.prompt}'"
            is JarvisIntent.CreateNote -> "Create note '${intent.title}'"
            is JarvisIntent.CreateTask -> "Add task '${intent.title}'"
            is JarvisIntent.QueryMemory -> "Search memory for '${intent.query}'"
            is JarvisIntent.ClearAllMemories -> "Erase all saved memories"
            is JarvisIntent.AgentPlan -> "Plan & execute autonomous agent goal: '${intent.goal}'"
            is JarvisIntent.GeneralQuery -> "Ask JARVIS: '${intent.query}'"
            is JarvisIntent.Unknown -> "Unknown command '${intent.rawInput}'"
        }
    }
}
