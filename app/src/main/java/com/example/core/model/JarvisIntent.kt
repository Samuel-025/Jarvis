package com.example.core.model

sealed class JarvisIntent {
    // Phase 2 Local Intents
    data class LaunchApp(val appQuery: String) : JarvisIntent()
    data class OpenSettings(val settingType: String) : JarvisIntent()
    data class Flashlight(val enable: Boolean) : JarvisIntent()
    data class VolumeControl(val action: VolumeAction, val levelPercent: Int? = null) : JarvisIntent()
    object BatteryStatus : JarvisIntent()
    object DateTimeQuery : JarvisIntent()
    
    // Emergency Stop
    object StopAll : JarvisIntent()
    
    // Phase 5 Phone Automation
    data class PhoneAction(val actionType: String, val params: Map<String, String> = emptyMap()) : JarvisIntent()
    
    // Phase 6 Vision
    data class VisionAnalyze(val prompt: String, val imageUri: String? = null) : JarvisIntent()
    
    // Phase 7 & 9 Memory and Personal OS
    data class CreateNote(val title: String, val content: String) : JarvisIntent()
    data class CreateTask(val title: String, val category: String = "General") : JarvisIntent()
    data class QueryMemory(val query: String) : JarvisIntent()
    object ClearAllMemories : JarvisIntent()
    
    // Phase 8 Bounded Agent Task
    data class AgentPlan(val goal: String) : JarvisIntent()
    
    // Phase 4 / Fallback Cloud query
    data class GeneralQuery(val query: String) : JarvisIntent()
    
    // Unknown or invalid
    data class Unknown(val rawInput: String, val reason: String = "No matching intent") : JarvisIntent()
}

enum class VolumeAction {
    UP,
    DOWN,
    MUTE,
    UNMUTE,
    SET_LEVEL
}
