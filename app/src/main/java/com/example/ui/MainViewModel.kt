package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.agent.AgentPlan
import com.example.core.audit.AuditEvent
import com.example.core.di.ServiceLocator
import com.example.core.integrations.IntegrationStatus
import com.example.core.memory.MemoryEntity
import com.example.core.memory.NoteEntity
import com.example.core.memory.RoutineEntity
import com.example.core.memory.TaskEntity
import com.example.core.model.CommandResult
import com.example.core.model.JarvisIntent
import com.example.core.model.PrivacyMode
import com.example.core.nlp.IntentClassifier
import com.example.core.nlp.ResponseFormatter
import com.example.core.safety.ConfirmationManager
import com.example.core.safety.ConfirmationRequest
import com.example.core.safety.EmergencyStop
import com.example.core.vision.VisionResult
import com.example.core.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val privacyMode: PrivacyMode = PrivacyMode.BALANCED,
    val voiceState: VoiceState = VoiceState.IDLE,
    val lastRecognizedSpeech: String = "",
    val voiceErrorMessage: String? = null,
    val isEmergencyStopped: Boolean = false,
    val pendingConfirmation: ConfirmationRequest? = null,
    val lastCommandResult: CommandResult? = null,
    val currentAgentPlan: AgentPlan? = null,
    val visionAnalysisResult: VisionResult? = null,
    val isProcessing: Boolean = false,
    val pendingRuntimePermission: String? = null,
    val pendingPermissionIntent: JarvisIntent? = null,
    val pendingPermissionSource: String? = null
)

class MainViewModel(
    private val serviceLocator: ServiceLocator
) : ViewModel() {

    private val privacyPreferences = serviceLocator.appContext.getSharedPreferences(
        "jarvis_app_preferences", android.content.Context.MODE_PRIVATE
    )
    private val _uiState = MutableStateFlow(
        MainUiState(
            privacyMode = runCatching {
                PrivacyMode.valueOf(
                    privacyPreferences.getString("privacy_mode", PrivacyMode.BALANCED.name)
                        ?: PrivacyMode.BALANCED.name
                )
            }.getOrDefault(PrivacyMode.BALANCED)
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var speechGeneration: Long = 0L

    // Observe Room data
    val notes: StateFlow<List<NoteEntity>> = serviceLocator.personalOsRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = serviceLocator.personalOsRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = serviceLocator.personalOsRepository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineEntity>> = serviceLocator.personalOsRepository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditEvents: StateFlow<List<AuditEvent>> = serviceLocator.auditEventRepository.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _integrations = MutableStateFlow<List<IntegrationStatus>>(emptyList())
    val integrations: StateFlow<List<IntegrationStatus>> = _integrations.asStateFlow()

    init {
        // Collect EmergencyStop state
        viewModelScope.launch {
            EmergencyStop.isStopped.collect { stopped ->
                _uiState.value = _uiState.value.copy(isEmergencyStopped = stopped)
            }
        }
        // Collect ConfirmationManager state
        viewModelScope.launch {
            ConfirmationManager.pendingRequest.collect { req ->
                _uiState.value = _uiState.value.copy(pendingConfirmation = req)
            }
        }
        refreshIntegrations()
    }

    fun setPrivacyMode(mode: PrivacyMode) {
        privacyPreferences.edit().putString("privacy_mode", mode.name).apply()
        _uiState.value = _uiState.value.copy(privacyMode = mode)
    }

    fun triggerEmergencyStop() {
        EmergencyStop.trigger("User initiated emergency stop via UI")
        speechGeneration++
        serviceLocator.textToSpeechAdapter.stop()
        serviceLocator.speechRecognizerAdapter.cancel()
        _uiState.value = _uiState.value.copy(
            voiceState = VoiceState.IDLE,
            isEmergencyStopped = true
        )
    }

    fun resetEmergencyStop() {
        EmergencyStop.reset()
        _uiState.value = _uiState.value.copy(isEmergencyStopped = false)
    }

    fun executeTextCommand(commandText: String) {
        val trimmed = commandText.trim()
        if (trimmed.isBlank()) return

        // A new command should not leave stale voice errors or an old result visible.
        _uiState.value = _uiState.value.copy(voiceErrorMessage = null, lastCommandResult = null)
        executeIntent(IntentClassifier.classify(trimmed), source = "USER_TEXT")
    }

    fun executeIntent(intent: JarvisIntent, source: String, isConfirmed: Boolean = false) {
        // Flashlight control needs CAMERA access. Request it from the visible UI only when
        // the user actually invokes the torch, including through voice commands.
        if (!EmergencyStop.isActive() && intent is JarvisIntent.Flashlight &&
            ContextCompat.checkSelfPermission(serviceLocator.appContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            _uiState.value = _uiState.value.copy(
                pendingRuntimePermission = Manifest.permission.CAMERA,
                pendingPermissionIntent = intent,
                pendingPermissionSource = source,
                isProcessing = false,
                voiceState = VoiceState.IDLE,
                lastCommandResult = null
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = serviceLocator.intentRouter.route(
                intent = intent,
                privacyMode = _uiState.value.privacyMode,
                source = source,
                isConfirmed = isConfirmed
            )

            _uiState.value = _uiState.value.copy(lastCommandResult = result)

            // Keep processing visible until the bounded agent has actually finished.
            if (intent is JarvisIntent.AgentPlan && result is CommandResult.Success) {
                val plan = serviceLocator.agentPlanner.createPlan(intent.goal)
                _uiState.value = _uiState.value.copy(currentAgentPlan = plan)
                serviceLocator.agentExecutor.executePlan(plan) { updatedPlan ->
                    _uiState.value = _uiState.value.copy(currentAgentPlan = updatedPlan)
                }
            }

            _uiState.value = _uiState.value.copy(isProcessing = false)
            if (result is CommandResult.Success && !result.audioFeedback.isNullOrBlank()) {
                val generation = ++speechGeneration
                if (!_uiState.value.isEmergencyStopped && _uiState.value.voiceState != VoiceState.LISTENING) {
                    _uiState.value = _uiState.value.copy(voiceState = VoiceState.SPEAKING)
                    serviceLocator.textToSpeechAdapter.speak(result.audioFeedback) {
                        if (generation == speechGeneration && _uiState.value.voiceState == VoiceState.SPEAKING) {
                            _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
                        }
                    }
                }
            } else if (source == "USER_VOICE" && _uiState.value.voiceState == VoiceState.PROCESSING) {
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
            }
        }
    }

    fun onRuntimePermissionResult(granted: Boolean) {
        val state = _uiState.value
        val pendingIntent = state.pendingPermissionIntent
        val source = state.pendingPermissionSource ?: "USER"
        _uiState.value = state.copy(
            pendingRuntimePermission = null,
            pendingPermissionIntent = null,
            pendingPermissionSource = null
        )
        if (pendingIntent == null) return
        if (granted) {
            executeIntent(pendingIntent, source = source)
        } else {
            _uiState.value = _uiState.value.copy(
                lastCommandResult = CommandResult.Error("Flashlight access was not granted. Jarvis can still use text, voice, AI, and other commands."),
                voiceState = VoiceState.IDLE,
                isProcessing = false
            )
        }
    }

    fun confirmPendingAction() {
        val approvedIntent = ConfirmationManager.approve()
        if (approvedIntent != null) {
            executeIntent(approvedIntent, source = "USER_CONFIRMED", isConfirmed = true)
        }
    }

    fun rejectPendingAction() {
        ConfirmationManager.reject()
        _uiState.value = _uiState.value.copy(
            lastCommandResult = CommandResult.Error("Action rejected by user.")
        )
    }

    // Voice lifecycle
    fun startVoiceListening() {
        if (EmergencyStop.isActive()) return
        speechGeneration++
        val generation = speechGeneration
        serviceLocator.textToSpeechAdapter.stop()
        _uiState.value = _uiState.value.copy(
            voiceState = VoiceState.LISTENING,
            voiceErrorMessage = null,
            lastCommandResult = null,
            lastRecognizedSpeech = ""
        )

        serviceLocator.speechRecognizerAdapter.startListening(
            onResult = { text ->
                if (generation != speechGeneration || EmergencyStop.isActive()) return@startListening
                _uiState.value = _uiState.value.copy(
                    voiceState = VoiceState.PROCESSING,
                    lastRecognizedSpeech = text,
                    voiceErrorMessage = null
                )
                // Execute recognized command without resetting voice state before it completes.
                executeIntent(IntentClassifier.classify(text), source = "USER_VOICE")
            },
            onError = { code, msg ->
                if (generation != speechGeneration) return@startListening
                val recoverableNoSpeech = code == SpeechRecognizer.ERROR_NO_MATCH ||
                    code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                _uiState.value = _uiState.value.copy(
                    voiceState = if (recoverableNoSpeech) VoiceState.IDLE else VoiceState.ERROR,
                    voiceErrorMessage = if (recoverableNoSpeech) {
                        "I didn't catch that. Tap the microphone to try again, or type your command."
                    } else msg
                )
            }
        )
    }

    fun stopVoiceListening() {
        // Ignore any late recognition callback after the user stops the current session.
        speechGeneration++
        serviceLocator.speechRecognizerAdapter.stopListening()
        if (_uiState.value.voiceState == VoiceState.LISTENING) {
            _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
        }
    }

    fun analyzeVisionBitmap(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = try {
                serviceLocator.visionProvider.analyze(bitmap, prompt, _uiState.value.privacyMode)
            } catch (e: Exception) {
                VisionResult.Error(e.message ?: "Vision analysis failed unexpectedly.")
            }
            _uiState.value = _uiState.value.copy(visionAnalysisResult = result, isProcessing = false)
            if (result is VisionResult.Success && !_uiState.value.isEmergencyStopped) {
                val generation = ++speechGeneration
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.SPEAKING)
                serviceLocator.textToSpeechAdapter.speak(result.description) {
                    if (generation == speechGeneration && _uiState.value.voiceState == VoiceState.SPEAKING) {
                        _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
                    }
                }
            }
        }
    }

    // Personal OS actions
    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            serviceLocator.personalOsRepository.saveNote(title, content)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            serviceLocator.personalOsRepository.deleteNote(id)
        }
    }

    fun addTask(title: String, category: String) {
        viewModelScope.launch {
            serviceLocator.personalOsRepository.addTask(title, category)
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            serviceLocator.personalOsRepository.toggleTask(task)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            serviceLocator.personalOsRepository.deleteTask(id)
        }
    }

    fun saveMemory(key: String, value: String) {
        viewModelScope.launch {
            serviceLocator.personalOsRepository.saveMemory(key, value)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            serviceLocator.personalOsRepository.deleteMemory(id)
        }
    }

    val aiProviderManager = serviceLocator.aiProviderManager

    val selectedProviderType: StateFlow<com.example.core.ai.provider.ProviderType> =
        aiProviderManager.selectedProviderType
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.core.ai.provider.ProviderType.GEMINI)

    val providerConfigurations: StateFlow<Map<com.example.core.ai.provider.ProviderType, com.example.core.ai.provider.ProviderConfiguration>> =
        aiProviderManager.configurations
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val providerConnectionStatuses: StateFlow<Map<com.example.core.ai.provider.ProviderType, com.example.core.ai.provider.ProviderConnectionStatus>> =
        aiProviderManager.connectionStatuses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val discoveredProviderModels: StateFlow<Map<com.example.core.ai.provider.ProviderType, List<com.example.core.ai.provider.ModelDescriptor>>> =
        aiProviderManager.discoveredModels
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _autoDetectMessage = MutableStateFlow<String?>(null)
    val autoDetectMessage: StateFlow<String?> = _autoDetectMessage.asStateFlow()

    fun setSelectedProvider(type: com.example.core.ai.provider.ProviderType) {
        aiProviderManager.setSelectedProvider(type)
    }

    fun saveProviderApiKey(type: com.example.core.ai.provider.ProviderType, key: String): Boolean {
        val saved = aiProviderManager.saveApiKey(type, key)
        if (saved) viewModelScope.launch { aiProviderManager.refreshModels(type) }
        return saved
    }

    fun refreshProviderModels(type: com.example.core.ai.provider.ProviderType, endpoint: String? = null) {
        viewModelScope.launch { aiProviderManager.refreshModels(type, endpointOverride = endpoint) }
    }

    fun autoDetectProviderFromKey(key: String, endpoint: String? = null) {
        viewModelScope.launch {
            _autoDetectMessage.value = "Checking your API key against supported providers…"
            val detected = aiProviderManager.autoDetectProvider(key.trim(), endpoint)
            _autoDetectMessage.value = if (detected == null) {
                "Could not identify this key. Check that it is valid, or choose the provider and endpoint manually."
            } else {
                "Detected ${detected.displayName}. Your key was saved securely and available models were loaded."
            }
        }
    }

    fun removeProviderApiKey(type: com.example.core.ai.provider.ProviderType): Boolean {
        return aiProviderManager.removeApiKey(type)
    }

    fun hasProviderApiKey(type: com.example.core.ai.provider.ProviderType): Boolean {
        return aiProviderManager.hasApiKey(type)
    }

    fun getMaskedProviderApiKey(type: com.example.core.ai.provider.ProviderType): String {
        return aiProviderManager.getMaskedApiKey(type)
    }

    fun updateProviderConfig(config: com.example.core.ai.provider.ProviderConfiguration) {
        aiProviderManager.updateConfiguration(config)
    }

    fun testProviderConnection(type: com.example.core.ai.provider.ProviderType) {
        viewModelScope.launch {
            aiProviderManager.testProviderConnection(type)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            serviceLocator.personalOsRepository.clearMemories()
        }
    }

    fun refreshIntegrations() {
        viewModelScope.launch {
            val list = serviceLocator.integrations.map { it.checkStatus() }
            _integrations.value = list
        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceLocator.speechRecognizerAdapter.destroy()
        serviceLocator.textToSpeechAdapter.shutdown()
    }

    companion object {
        fun provideFactory(serviceLocator: ServiceLocator): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(serviceLocator) as T
                }
            }
    }
}
