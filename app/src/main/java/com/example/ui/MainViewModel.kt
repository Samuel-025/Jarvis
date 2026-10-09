package com.example.ui

import android.graphics.Bitmap
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
    val isProcessing: Boolean = false
)

class MainViewModel(
    private val serviceLocator: ServiceLocator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

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
        _uiState.value = _uiState.value.copy(privacyMode = mode)
    }

    fun triggerEmergencyStop() {
        EmergencyStop.trigger("User initiated emergency stop via UI")
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

        // Clear any previous stale voice error
        _uiState.value = _uiState.value.copy(
            voiceErrorMessage = null,
            isProcessing = true
        )

        viewModelScope.launch {
            val intent = IntentClassifier.classify(trimmed)
            executeIntent(intent, source = "USER_TEXT")
        }
    }

    fun executeIntent(intent: JarvisIntent, source: String, isConfirmed: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = serviceLocator.intentRouter.route(
                intent = intent,
                privacyMode = _uiState.value.privacyMode,
                source = source,
                isConfirmed = isConfirmed
            )

            _uiState.value = _uiState.value.copy(
                lastCommandResult = result,
                isProcessing = false
            )

            // TTS feedback if result has audio feedback
            if (result is CommandResult.Success && result.audioFeedback != null) {
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.SPEAKING)
                serviceLocator.textToSpeechAdapter.speak(result.audioFeedback) {
                    _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
                }
            }

            // If intent was AgentPlan, launch the executor
            if (intent is JarvisIntent.AgentPlan && result is CommandResult.Success) {
                val plan = serviceLocator.agentPlanner.createPlan(intent.goal)
                _uiState.value = _uiState.value.copy(currentAgentPlan = plan)
                serviceLocator.agentExecutor.executePlan(plan) { updatedPlan ->
                    _uiState.value = _uiState.value.copy(currentAgentPlan = updatedPlan)
                }
            }
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
        serviceLocator.textToSpeechAdapter.stop()
        _uiState.value = _uiState.value.copy(
            voiceState = VoiceState.LISTENING,
            voiceErrorMessage = null
        )

        serviceLocator.speechRecognizerAdapter.startListening(
            onResult = { text ->
                _uiState.value = _uiState.value.copy(
                    voiceState = VoiceState.PROCESSING,
                    lastRecognizedSpeech = text,
                    voiceErrorMessage = null
                )
                // Execute recognized command
                viewModelScope.launch {
                    val intent = IntentClassifier.classify(text)
                    executeIntent(intent, source = "USER_VOICE")
                    _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
                }
            },
            onError = { code, msg ->
                _uiState.value = _uiState.value.copy(
                    voiceState = VoiceState.ERROR,
                    voiceErrorMessage = msg
                )
            }
        )
    }

    fun stopVoiceListening() {
        serviceLocator.speechRecognizerAdapter.stopListening()
        if (_uiState.value.voiceState == VoiceState.LISTENING) {
            _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
        }
    }

    fun analyzeVisionBitmap(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = serviceLocator.visionProvider.analyze(bitmap, prompt)
            _uiState.value = _uiState.value.copy(
                visionAnalysisResult = result,
                isProcessing = false
            )
            if (result is VisionResult.Success) {
                serviceLocator.textToSpeechAdapter.speak(result.description)
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

    fun setSelectedProvider(type: com.example.core.ai.provider.ProviderType) {
        aiProviderManager.setSelectedProvider(type)
    }

    fun saveProviderApiKey(type: com.example.core.ai.provider.ProviderType, key: String): Boolean {
        return aiProviderManager.saveApiKey(type, key)
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
