package com.example.core.nlp

import com.example.core.model.JarvisIntent
import com.example.core.model.VolumeAction
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object IntentClassifier {
    fun classify(rawInput: String): JarvisIntent {
        val normalized = InputNormalizer.normalize(rawInput)
        if (normalized.isBlank()) return JarvisIntent.Unknown(rawInput, "Input was empty or blank")

        // Emergency-stop phrases are intentionally checked before all other commands.
        if (normalized in setOf(
                "stop", "emergency stop", "halt", "abort", "cancel everything", "stop all",
                "sab band karo", "sab kuch band karo", "ruk jao", "bas karo", "रुको", "सब बंद करो"
            )
        ) return JarvisIntent.StopAll

        // Flashlight / torch — English and common Hinglish/Hindi phrases.
        if (normalized.contains("turn on flashlight") || normalized.contains("enable flashlight") ||
            normalized == "flashlight on" || normalized == "torch on" ||
            normalized in setOf("torch chalu karo", "torch chalu kar do", "torch jalao", "flashlight chalu karo", "flashlight jalao", "torch on karo", "टॉर्च चालू करो", "टॉर्च जलाओ", "लाइट चालू करो")
        ) return JarvisIntent.Flashlight(true)
        if (normalized.contains("turn off flashlight") || normalized.contains("disable flashlight") ||
            normalized == "flashlight off" || normalized == "torch off" ||
            normalized in setOf("torch band karo", "torch band kar do", "torch bujhao", "flashlight band karo", "torch off karo", "टॉर्च बंद करो", "टॉर्च बुझाओ", "लाइट बंद करो")
        ) return JarvisIntent.Flashlight(false)

        // Unmute must be checked before mute because the word "unmute" contains "mute".
        if (normalized.contains("unmute") || normalized in setOf("awaaz chalu karo", "sound chalu karo", "अनम्यूट करो", "आवाज चालू करो")) {
            return JarvisIntent.VolumeControl(VolumeAction.UNMUTE)
        }
        if (normalized.contains("volume up") || normalized.contains("increase volume") || normalized.contains("louder") ||
            normalized in setOf("volume badhao", "awaz badhao", "awaaz badhao", "sound badhao", "आवाज बढ़ाओ", "वॉल्यूम बढ़ाओ")
        ) return JarvisIntent.VolumeControl(VolumeAction.UP)
        if (normalized.contains("volume down") || normalized.contains("decrease volume") || normalized.contains("quieter") ||
            normalized in setOf("volume kam karo", "awaz kam karo", "awaaz kam karo", "sound kam karo", "आवाज कम करो", "वॉल्यूम कम करो")
        ) return JarvisIntent.VolumeControl(VolumeAction.DOWN)
        if (normalized.contains("mute") || normalized.contains("silence") || normalized in setOf("mute karo", "awaaz band karo", "आवाज बंद करो", "म्यूट करो")) {
            return JarvisIntent.VolumeControl(VolumeAction.MUTE)
        }
        val volumePercentMatch = Regex("set volume (to )?(\\d+)%?").find(normalized)
        if (volumePercentMatch != null) {
            val level = volumePercentMatch.groupValues[2].toIntOrNull()
            if (level != null) return JarvisIntent.VolumeControl(VolumeAction.SET_LEVEL, level.coerceIn(0, 100))
        }

        // Battery and date/time queries.
        if (normalized.contains("battery") || normalized.contains("power level") || normalized.contains("charge level") ||
            normalized in setOf("battery kitni hai", "battery kitna hai", "battery percent batao", "battery percentage batao", "battery dikhao", "बैटरी कितनी है", "बैटरी बताओ")
        ) return JarvisIntent.BatteryStatus
        if (normalized.contains("what time") || normalized.contains("current time") || normalized.contains("what is the time") ||
            normalized.contains("what day") || normalized.contains("today's date") || normalized.contains("what is the date") ||
            normalized in setOf("time kya hua hai", "kitne baje hain", "kitne baje hai", "samay kya hai", "abhi time kya hai", "aaj ki date kya hai", "aaj ka date kya hai", "aaj kaun sa din hai", "अभी कितने बजे हैं", "समय क्या है", "आज की तारीख क्या है", "आज कौन सा दिन है")
        ) return JarvisIntent.DateTimeQuery

        // Phone actions use Android intents only: calls open the dialer and messages open a draft.
        // Jarvis never silently places a call or sends an SMS.
        val dialMatch = Regex("(?i)^(?:call|dial)\\s+([+()0-9 .-]{3,})$").find(rawInput.trim())
        if (dialMatch != null) {
            val number = dialMatch.groupValues[1].filter { it.isDigit() || it == '+' }
            if (number.count { it.isDigit() } >= 3) {
                return JarvisIntent.PhoneAction("dial_phone", mapOf("number" to number))
            }
        }
        val smsMatch = Regex("(?i)^(?:text|sms|message|send sms to|send message to)\\s+([+()0-9 .-]{3,})\\s+(?:saying|message|that)\\s+(.+)$").find(rawInput.trim())
        if (smsMatch != null) {
            val number = smsMatch.groupValues[1].filter { it.isDigit() || it == '+' }
            val message = smsMatch.groupValues[2].trim()
            if (number.count { it.isDigit() } >= 3 && message.isNotBlank()) {
                return JarvisIntent.PhoneAction("send_sms_draft", mapOf("number" to number, "message" to message))
            }
        }

        // Timers are handed to the user's Clock app; reminders/scheduled background routines
        // are not claimed as implemented until a notification-backed scheduler exists.
        val timerMatch = Regex("(?i)^(?:set )?timer for (\\d+)\\s+(seconds?|minutes?|hours?)(?:\\s+(?:called|named|for)\\s+(.+))?$").find(rawInput.trim())
        if (timerMatch != null) {
            val amount = timerMatch.groupValues[1].toLongOrNull()
            val unit = timerMatch.groupValues[2].lowercase()
            val multiplier = when {
                unit.startsWith("hour") -> 3600L
                unit.startsWith("minute") -> 60L
                else -> 1L
            }
            val seconds = amount?.times(multiplier)?.coerceIn(1L, 86400L)
            if (seconds != null) {
                val label = timerMatch.groupValues[3].ifBlank { "JARVIS Timer" }
                return JarvisIntent.PhoneAction("set_timer", mapOf("seconds" to seconds.toString(), "message" to label))
            }
        }

        // Web and YouTube searches open a real URL in the user's browser/app; no search is fabricated.
        val normalizedYoutubeQuery = when {
            normalized.startsWith("search youtube for ") -> normalized.removePrefix("search youtube for ")
            normalized.startsWith("youtube search for ") -> normalized.removePrefix("youtube search for ")
            normalized.startsWith("open youtube for ") -> normalized.removePrefix("open youtube for ")
            normalized.startsWith("play ") && normalized.endsWith(" on youtube") -> normalized.removePrefix("play ").removeSuffix(" on youtube")
            normalized.startsWith("search youtube ") -> normalized.removePrefix("search youtube ")
            else -> ""
        }.trim()
        if (normalizedYoutubeQuery.isNotBlank()) {
            val query = URLEncoder.encode(normalizedYoutubeQuery, StandardCharsets.UTF_8.name())
            return JarvisIntent.PhoneAction("open_web_url", mapOf("url" to "https://www.youtube.com/results?search_query=$query"))
        }
        val normalizedWebQuery = when {
            normalized.startsWith("search web for ") -> normalized.removePrefix("search web for ")
            normalized.startsWith("search for ") -> normalized.removePrefix("search for ")
            normalized.startsWith("google ") -> normalized.removePrefix("google ")
            else -> ""
        }.trim()
        if (normalizedWebQuery.isNotBlank()) {
            val query = URLEncoder.encode(normalizedWebQuery, StandardCharsets.UTF_8.name())
            return JarvisIntent.PhoneAction("open_web_url", mapOf("url" to "https://www.google.com/search?q=$query"))
        }

        // App launch aliases. Keep the target narrow so arbitrary speech isn't mistaken for a launch.
        val launchAliases = mapOf(
            "youtube kholo" to "youtube", "youtube khol do" to "youtube", "youtube khol" to "youtube", "यूट्यूब खोलो" to "youtube",
            "chrome kholo" to "chrome", "chrome khol do" to "chrome", "क्रोम खोलो" to "chrome",
            "camera kholo" to "camera", "camera khol do" to "camera", "कैमरा खोलो" to "camera",
            "settings kholo" to "settings", "settings khol do" to "settings", "सेटिंग्स खोलो" to "settings"
        )
        launchAliases[normalized]?.let { return JarvisIntent.LaunchApp(it) }
        if (normalized.startsWith("open ") || normalized.startsWith("launch ")) {
            val target = normalized.removePrefix("open ").removePrefix("launch ").trim()
            if (target.contains("settings") || target.contains("wifi") || target.contains("bluetooth") || target.contains("display")) {
                return JarvisIntent.OpenSettings(target)
            }
            return JarvisIntent.LaunchApp(target)
        }
        if (normalized.startsWith("settings") || normalized.contains("open settings")) {
            val target = normalized.replace("open", "").replace("settings", "").trim()
            return JarvisIntent.OpenSettings(if (target.isBlank()) "general" else target)
        }

        // Personal OS: strip the original prefix case-insensitively so capitalized speech is safe.
        val notePrefixes = listOf("take a note", "create note", "note")
        if (notePrefixes.any { normalized == it || normalized.startsWith("$it ") || normalized.startsWith("$it:") }) {
            val prefix = notePrefixes.first { normalized == it || normalized.startsWith("$it ") || normalized.startsWith("$it:") }
            val content = rawInput.trim().drop(prefix.length).trim().removePrefix(":").removePrefix("-").trim()
            return JarvisIntent.CreateNote(title = if (content.length > 30) content.take(30) + "..." else content, content = content)
        }
        val taskPrefixes = listOf("add task", "task", "todo", "to do")
        if (taskPrefixes.any { normalized == it || normalized.startsWith("$it ") || normalized.startsWith("$it:") }) {
            val prefix = taskPrefixes.first { normalized == it || normalized.startsWith("$it ") || normalized.startsWith("$it:") }
            val title = rawInput.trim().drop(prefix.length).trim().removePrefix(":").removePrefix("-").trim()
            if (title.isNotBlank()) return JarvisIntent.CreateTask(title = title)
        }
        val memoryPrefixes = listOf("remember that", "remember", "save memory", "store memory", "memory")
        if (memoryPrefixes.any { normalized == it || normalized.startsWith("$it ") || normalized.startsWith("$it:") }) {
            val prefix = memoryPrefixes.first { normalized == it || normalized.startsWith("$it ") || normalized.startsWith("$it:") }
            val fact = rawInput.trim().drop(prefix.length).trim().removePrefix(":").removePrefix("-").trim()
            if (fact.isNotBlank()) return JarvisIntent.CreateNote(title = "Memory", content = fact)
        }
        if (normalized == "clear memories" || normalized == "delete all memories" || normalized == "clear all memories") {
            return JarvisIntent.ClearAllMemories
        }
        if (normalized.startsWith("recall ") || normalized.startsWith("what did i note") || normalized.startsWith("search notes ") ||
            normalized.startsWith("yaad dilao") || normalized.startsWith("yaad karo") || normalized.startsWith("याद दिलाओ")
        ) {
            val query = when {
                normalized.startsWith("recall ") -> rawInput.trim().drop("recall ".length).trim()
                normalized.startsWith("search notes ") -> rawInput.trim().drop("search notes ".length).trim()
                normalized.startsWith("yaad dilao ") -> rawInput.trim().drop("yaad dilao ".length).trim()
                normalized.startsWith("yaad karo ") -> rawInput.trim().drop("yaad karo ".length).trim()
                normalized.startsWith("याद दिलाओ ") -> rawInput.trim().drop("याद दिलाओ ".length).trim()
                else -> normalized.removePrefix("what did i note").trim()
            }
            return JarvisIntent.QueryMemory(query)
        }

        // Vision and bounded agent requests.
        if (normalized.startsWith("see ") || normalized.startsWith("analyze image") || normalized.startsWith("look at this")) {
            return JarvisIntent.VisionAnalyze(prompt = rawInput)
        }
        if (normalized.startsWith("agent ") || normalized.startsWith("plan ") || normalized.startsWith("execute routine ")) {
            val goal = rawInput.trim().replaceFirst(Regex("(?i)^(agent|plan|execute routine)\\s*"), "").trim()
            return JarvisIntent.AgentPlan(goal = goal)
        }
        return JarvisIntent.GeneralQuery(rawInput.trim())
    }
}
