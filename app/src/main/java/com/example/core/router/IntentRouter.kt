package com.example.core.router

import com.example.core.ai.AIBrain
import com.example.core.ai.AIResponse
import com.example.core.audit.AuditEvent
import com.example.core.audit.AuditEventRepository
import com.example.core.engine.LocalCommandEngine
import com.example.core.memory.PersonalOsRepository
import com.example.core.model.CommandResult
import com.example.core.model.ErrorType
import com.example.core.model.JarvisIntent
import com.example.core.model.PrivacyMode
import com.example.core.model.RiskLevel
import com.example.core.phone.PhoneAutomationRegistry
import com.example.core.safety.ConfirmationManager
import com.example.core.safety.EmergencyStop
import com.example.core.safety.RiskEngine
import com.example.core.tools.ToolRegistry

class IntentRouter(
    private val localCommandEngine: LocalCommandEngine,
    private val phoneRegistry: PhoneAutomationRegistry,
    private val personalOsRepository: PersonalOsRepository,
    private val aiBrain: AIBrain,
    private val toolRegistry: ToolRegistry,
    private val auditEventRepository: AuditEventRepository
) {
    suspend fun route(
        intent: JarvisIntent,
        privacyMode: PrivacyMode = PrivacyMode.BALANCED,
        source: String = "USER",
        isConfirmed: Boolean = false
    ): CommandResult {
        // 1. Mandatory EmergencyStop check
        if (EmergencyStop.isActive() && intent !is JarvisIntent.StopAll) {
            val errResult = CommandResult.Error(
                message = "Emergency Stop is active. Operation halted.",
                errorType = ErrorType.EMERGENCY_STOPPED,
                recoverySuggestion = "Tap 'Reset Emergency Stop' to resume normal operation."
            )
            logAudit(intent, source, false, errResult.message, RiskLevel.HIGH.name)
            return errResult
        }

        // 2. Risk assessment
        val assessment = RiskEngine.assess(intent, privacyMode)
        if (assessment.requiresConfirmation && !isConfirmed) {
            val confirmation = ConfirmationManager.requestConfirmation(assessment.reason, intent)
            val reqResult = CommandResult.RequiresConfirmation(
                actionId = confirmation.id,
                confirmationPrompt = assessment.reason,
                pendingIntent = intent
            )
            logAudit(intent, source, true, "Requires confirmation: ${assessment.reason}", assessment.level.name)
            return reqResult
        }

        // 3. Execution routing
        val result = try {
            when (intent) {
                is JarvisIntent.StopAll -> {
                    EmergencyStop.trigger()
                    CommandResult.Success(
                        message = "EMERGENCY STOP engaged. All operations immediately suspended.",
                        audioFeedback = "Emergency stop engaged."
                    )
                }

                is JarvisIntent.Flashlight,
                is JarvisIntent.VolumeControl,
                is JarvisIntent.BatteryStatus,
                is JarvisIntent.DateTimeQuery,
                is JarvisIntent.LaunchApp,
                is JarvisIntent.OpenSettings -> {
                    localCommandEngine.execute(intent)
                }

                is JarvisIntent.PhoneAction -> {
                    phoneRegistry.executeAction(intent.actionType, intent.params)
                }

                is JarvisIntent.CreateNote -> {
                    if (intent.title.equals("Memory", ignoreCase = true)) {
                        val fact = intent.content.trim()
                        personalOsRepository.saveMemory(
                            key = fact.take(80).ifBlank { "remembered_fact" },
                            value = fact,
                            category = "personal"
                        )
                        CommandResult.Success(
                            message = "Remembered: '$fact'",
                            audioFeedback = "I'll remember that."
                        )
                    } else {
                        personalOsRepository.saveNote(intent.title, intent.content)
                        CommandResult.Success(
                            message = "Note saved: '${intent.title}'",
                            audioFeedback = "Note saved."
                        )
                    }
                }

                is JarvisIntent.CreateTask -> {
                    personalOsRepository.addTask(intent.title, intent.category)
                    CommandResult.Success(
                        message = "Task added: '${intent.title}'",
                        audioFeedback = "Task added."
                    )
                }

                is JarvisIntent.QueryMemory -> {
                    val results = personalOsRepository.searchMemories(intent.query)
                    if (results.isNotEmpty()) {
                        val text = results.joinToString("\n") { "• ${it.key}: ${it.value}" }
                        CommandResult.Success(
                            message = "Found memories:\n$text",
                            audioFeedback = "Found ${results.size} matching memories."
                        )
                    } else {
                        CommandResult.Success(
                            message = "No memories found matching '${intent.query}'",
                            audioFeedback = "No memories found."
                        )
                    }
                }

                is JarvisIntent.ClearAllMemories -> {
                    personalOsRepository.clearMemories()
                    CommandResult.Success(
                        message = "All memories permanently cleared.",
                        audioFeedback = "All memories erased."
                    )
                }

                is JarvisIntent.VisionAnalyze -> {
                    CommandResult.Error(
                        message = "Vision analysis needs a real photo. Open Settings → Agent & Vision and use Take Photo & Analyze. Cloud image analysis requires Cloud privacy mode.",
                        errorType = ErrorType.INVALID_INPUT
                    )
                }

                is JarvisIntent.AgentPlan -> {
                    CommandResult.Success(
                        message = "Agent plan registered for goal '${intent.goal}'. Executing in Bounded Agent pipeline.",
                        audioFeedback = "Executing agent plan."
                    )
                }

                is JarvisIntent.GeneralQuery -> {
                    // AIProviderManager enforces local-only behavior in STRICT mode.
                    // Do not claim success with a fabricated response when a provider fails.
                    when (val aiResponse = aiBrain.query(intent.query, privacyMode)) {
                        is AIResponse.Success -> CommandResult.Success(
                            message = aiResponse.text,
                            audioFeedback = aiResponse.text
                        )
                        is AIResponse.Error -> CommandResult.Error(
                            message = aiResponse.message,
                            errorType = if (aiResponse.isOffline) ErrorType.PRIVACY_RESTRICTED else ErrorType.EXECUTION_FAILED
                        )
                    }
                }

                is JarvisIntent.Unknown -> {
                    CommandResult.Error(
                        message = "I didn't understand '${intent.rawInput}'. Try 'turn on flashlight', 'battery', 'open youtube', or 'add task'.",
                        errorType = ErrorType.INVALID_INPUT
                    )
                }
            }
        } catch (e: Exception) {
            CommandResult.Error(
                message = "Routing failed with exception: ${e.message}",
                errorType = ErrorType.EXECUTION_FAILED
            )
        }

        // 4. Record audit event
        val isSuccess = result is CommandResult.Success
        val details = when (result) {
            is CommandResult.Success -> result.message
            is CommandResult.Error -> result.message
            is CommandResult.RequiresConfirmation -> "Pending Confirmation"
        }
        logAudit(intent, source, isSuccess, details, assessment.level.name)

        return result
    }

    private fun logAudit(
        intent: JarvisIntent,
        source: String,
        success: Boolean,
        details: String,
        riskLevel: String
    ) {
        auditEventRepository.log(
            AuditEvent(
                source = source,
                action = intent::class.simpleName ?: "UnknownIntent",
                details = details,
                success = success,
                riskLevel = riskLevel
            )
        )
    }
}
