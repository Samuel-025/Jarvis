package com.example.core.nlp

import com.example.core.model.JarvisIntent
import com.example.core.model.VolumeAction

object IntentClassifier {
    fun classify(rawInput: String): JarvisIntent {
        val normalized = InputNormalizer.normalize(rawInput)
        if (normalized.isBlank()) {
            return JarvisIntent.Unknown(rawInput, "Input was empty or blank")
        }

        // 1. Emergency stop check
        if (normalized == "stop" || normalized == "emergency stop" || normalized == "halt" ||
            normalized == "abort" || normalized == "cancel everything" || normalized == "stop all"
        ) {
            return JarvisIntent.StopAll
        }

        // 2. Flashlight / Torch
        if (normalized.contains("turn on flashlight") || normalized.contains("torch on") ||
            normalized == "flashlight on" || normalized.contains("enable flashlight")
        ) {
            return JarvisIntent.Flashlight(true)
        }
        if (normalized.contains("turn off flashlight") || normalized.contains("torch off") ||
            normalized == "flashlight off" || normalized.contains("disable flashlight")
        ) {
            return JarvisIntent.Flashlight(false)
        }

        // 3. Volume
        if (normalized.contains("volume up") || normalized.contains("increase volume") || normalized.contains("louder")) {
            return JarvisIntent.VolumeControl(VolumeAction.UP)
        }
        if (normalized.contains("volume down") || normalized.contains("decrease volume") || normalized.contains("quieter")) {
            return JarvisIntent.VolumeControl(VolumeAction.DOWN)
        }
        if (normalized.contains("mute") || normalized.contains("silence")) {
            return JarvisIntent.VolumeControl(VolumeAction.MUTE)
        }
        if (normalized.contains("unmute")) {
            return JarvisIntent.VolumeControl(VolumeAction.UNMUTE)
        }
        val volumePercentMatch = Regex("set volume (to )?(\\d+)%?").find(normalized)
        if (volumePercentMatch != null) {
            val level = volumePercentMatch.groupValues[2].toIntOrNull()
            if (level != null) {
                return JarvisIntent.VolumeControl(VolumeAction.SET_LEVEL, level)
            }
        }

        // 4. Battery status
        if (normalized.contains("battery") || normalized.contains("power level") || normalized.contains("charge level")) {
            return JarvisIntent.BatteryStatus
        }

        // 5. Date and Time
        if (normalized.contains("what time") || normalized.contains("current time") ||
            normalized.contains("what is the time") || normalized.contains("what day") ||
            normalized.contains("today's date") || normalized.contains("what is the date")
        ) {
            return JarvisIntent.DateTimeQuery
        }

        // 6. Launch App (e.g., "open youtube", "launch camera", "open chrome")
        if (normalized.startsWith("open ") || normalized.startsWith("launch ")) {
            val target = normalized.removePrefix("open ").removePrefix("launch ").trim()
            if (target.contains("settings") || target.contains("wifi") || target.contains("bluetooth") || target.contains("display")) {
                return JarvisIntent.OpenSettings(target)
            }
            return JarvisIntent.LaunchApp(target)
        }

        // 7. Settings explicit
        if (normalized.startsWith("settings") || normalized.contains("open settings")) {
            val target = normalized.replace("open", "").replace("settings", "").trim()
            return JarvisIntent.OpenSettings(if (target.isBlank()) "general" else target)
        }

        // 8. Personal OS / Notes / Tasks
        if (normalized.startsWith("note ") || normalized.startsWith("take a note ") || normalized.startsWith("create note ")) {
            val content = rawInput.substringAfter("note", "").trim().removePrefix(":").removePrefix("-").trim()
            return JarvisIntent.CreateNote(
                title = if (content.length > 30) content.take(30) + "..." else content,
                content = content
            )
        }
        if (normalized.startsWith("task ") || normalized.startsWith("add task ") || normalized.startsWith("todo ")) {
            val taskText = rawInput.substringAfter("task", "").substringAfter("todo", "").trim()
            return JarvisIntent.CreateTask(title = taskText)
        }
        if (normalized.startsWith("remember ") || normalized.startsWith("save memory ")) {
            val mem = rawInput.substringAfter("remember", "").substringAfter("memory", "").trim()
            return JarvisIntent.CreateNote(title = "Memory", content = mem)
        }
        if (normalized == "clear memories" || normalized == "delete all memories") {
            return JarvisIntent.ClearAllMemories
        }
        if (normalized.startsWith("recall ") || normalized.startsWith("what did i note") || normalized.startsWith("search notes ")) {
            val query = normalized.removePrefix("recall ").removePrefix("search notes ").trim()
            return JarvisIntent.QueryMemory(query)
        }

        // 9. Vision
        if (normalized.startsWith("see ") || normalized.startsWith("analyze image") || normalized.startsWith("look at this")) {
            return JarvisIntent.VisionAnalyze(prompt = rawInput)
        }

        // 10. Agent Plan
        if (normalized.startsWith("agent ") || normalized.startsWith("plan ") || normalized.startsWith("execute routine ")) {
            val goal = rawInput.substringAfter("agent").substringAfter("plan").substringAfter("routine").trim()
            return JarvisIntent.AgentPlan(goal = goal)
        }

        // Fallback: General query to AI Brain / Local knowledge
        return JarvisIntent.GeneralQuery(rawInput.trim())
    }
}
