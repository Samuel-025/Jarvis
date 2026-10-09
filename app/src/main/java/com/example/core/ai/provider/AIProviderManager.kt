package com.example.core.ai.provider

import android.content.Context
import com.example.core.ai.AIBrain
import com.example.core.ai.AIResponse
import com.example.core.ai.storage.ApiKeyStorage
import com.example.core.model.PrivacyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AIProviderManager(
    private val apiKeyStorage: ApiKeyStorage,
    context: Context? = null
) : AIBrain {

    companion object {
        private const val PREFS_NAME = "jarvis_ai_provider_preferences"
        private const val SELECTED_PROVIDER_KEY = "selected_provider_type"
    }

    private val providers = mutableMapOf<ProviderType, AIProvider>()
    private val settingsPrefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configurations = MutableStateFlow<Map<ProviderType, ProviderConfiguration>>(emptyMap())
    val configurations: StateFlow<Map<ProviderType, ProviderConfiguration>> = _configurations.asStateFlow()

    private val _selectedProviderType = MutableStateFlow(ProviderType.GEMINI)
    val selectedProviderType: StateFlow<ProviderType> = _selectedProviderType.asStateFlow()

    private val _connectionStatuses = MutableStateFlow<Map<ProviderType, ProviderConnectionStatus>>(emptyMap())
    val connectionStatuses: StateFlow<Map<ProviderType, ProviderConnectionStatus>> = _connectionStatuses.asStateFlow()

    private val _discoveredModels = MutableStateFlow<Map<ProviderType, List<ModelDescriptor>>>(emptyMap())
    val discoveredModels: StateFlow<Map<ProviderType, List<ModelDescriptor>>> = _discoveredModels.asStateFlow()

    init {
        // Register default providers
        val gemini = GeminiProviderAdapter()
        val openAI = OpenAIProviderAdapter(OpenAIProviderAdapter.defaultDescriptor(ProviderType.OPENAI))
        val customOpenAI = OpenAIProviderAdapter(
            OpenAIProviderAdapter.defaultDescriptor(
                ProviderType.OPENAI_COMPATIBLE,
                "https://api.groq.com/openai/v1/"
            )
        )
        val local = LocalOfflineProvider()

        registerProvider(gemini)
        registerProvider(openAI)
        registerProvider(customOpenAI)
        registerProvider(local)

        // Initialize default configurations
        val initConfigs = mutableMapOf<ProviderType, ProviderConfiguration>()
        initConfigs[ProviderType.GEMINI] = ProviderConfiguration(
            type = ProviderType.GEMINI,
            selectedModelId = gemini.descriptor.defaultModelId
        )
        initConfigs[ProviderType.OPENAI] = ProviderConfiguration(
            type = ProviderType.OPENAI,
            selectedModelId = openAI.descriptor.defaultModelId
        )
        initConfigs[ProviderType.OPENAI_COMPATIBLE] = ProviderConfiguration(
            type = ProviderType.OPENAI_COMPATIBLE,
            selectedModelId = customOpenAI.descriptor.defaultModelId,
            customEndpoint = "https://api.groq.com/openai/v1/"
        )
        initConfigs[ProviderType.LOCAL_OFFLINE] = ProviderConfiguration(
            type = ProviderType.LOCAL_OFFLINE,
            selectedModelId = local.descriptor.defaultModelId
        )
        // Restore provider and model preferences before the UI observes this manager.
        settingsPrefs?.let { prefs ->
            ProviderType.values().forEach { type ->
                val base = initConfigs[type] ?: return@forEach
                val modelId = prefs.getString(modelKey(type), null)?.takeIf { it.isNotBlank() }
                val endpoint = prefs.getString(endpointKey(type), base.customEndpoint)
                val enabled = prefs.getBoolean(enabledKey(type), base.isEnabled)
                initConfigs[type] = base.copy(
                    selectedModelId = modelId ?: base.selectedModelId,
                    customEndpoint = endpoint,
                    isEnabled = enabled
                )
            }
            _selectedProviderType.value = prefs.getString(SELECTED_PROVIDER_KEY, null)
                ?.let { saved -> runCatching { ProviderType.valueOf(saved) }.getOrNull() }
                ?: ProviderType.GEMINI
            _discoveredModels.value = ProviderType.values().mapNotNull { type ->
                val raw = prefs.getString(discoveredModelsKey(type), null) ?: return@mapNotNull null
                val models = decodeModels(raw)
                if (models.isEmpty()) null else type to models
            }.toMap()
        }
        _configurations.value = initConfigs
    }

    private fun modelKey(type: ProviderType) = "${type.name}_selected_model"
    private fun endpointKey(type: ProviderType) = "${type.name}_custom_endpoint"
    private fun enabledKey(type: ProviderType) = "${type.name}_enabled"
    private fun discoveredModelsKey(type: ProviderType) = "${type.name}_discovered_models"

    private fun persistConfiguration(config: ProviderConfiguration) {
        val prefs = settingsPrefs ?: return
        val editor = prefs.edit()
            .putString(modelKey(config.type), config.selectedModelId)
            .putBoolean(enabledKey(config.type), config.isEnabled)
        if (config.customEndpoint == null) editor.remove(endpointKey(config.type))
        else editor.putString(endpointKey(config.type), config.customEndpoint)
        editor.apply()
    }

    private fun persistDiscoveredModels(type: ProviderType, models: List<ModelDescriptor>) {
        val prefs = settingsPrefs ?: return
        val array = JSONArray()
        models.forEach { model ->
            val item = JSONObject()
                .put("id", model.id)
                .put("displayName", model.displayName)
                .put("capabilities", JSONArray(model.capabilities.map { it.name }))
            model.defaultEndpoint?.let { item.put("defaultEndpoint", it) }
            array.put(item)
        }
        prefs.edit().putString(discoveredModelsKey(type), array.toString()).apply()
    }

    private fun decodeModels(raw: String): List<ModelDescriptor> = try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val caps = item.optJSONArray("capabilities")
            val capabilities = if (caps == null) emptySet() else (0 until caps.length()).mapNotNull { i ->
                runCatching { ProviderCapability.valueOf(caps.optString(i)) }.getOrNull()
            }.toSet()
            ModelDescriptor(
                id = id,
                displayName = item.optString("displayName", id),
                capabilities = capabilities,
                defaultEndpoint = item.optString("defaultEndpoint").takeIf { it.isNotBlank() }
            )
        }
    } catch (_: Exception) { emptyList() }

    fun registerProvider(provider: AIProvider) {
        providers[provider.descriptor.type] = provider
    }

    fun getAllProviders(): List<AIProvider> = providers.values.toList()

    fun getProvider(type: ProviderType): AIProvider? = providers[type]

    fun setSelectedProvider(type: ProviderType) {
        _selectedProviderType.value = type
        settingsPrefs?.edit()?.putString(SELECTED_PROVIDER_KEY, type.name)?.apply()
    }

    fun updateConfiguration(config: ProviderConfiguration) {
        val current = _configurations.value.toMutableMap()
        current[config.type] = config
        _configurations.value = current
        persistConfiguration(config)
    }

    fun saveApiKey(type: ProviderType, key: String): Boolean {
        val saved = apiKeyStorage.saveKey(type.name, key)
        if (saved) {
            updateStatus(type, ProviderConnectionStatus(type, ConnectionState.NOT_CONFIGURED, "Key saved. Tap Test Connection."))
        }
        return saved
    }

    fun removeApiKey(type: ProviderType): Boolean {
        val removed = apiKeyStorage.removeKey(type.name)
        if (removed) {
            updateStatus(type, ProviderConnectionStatus(type, ConnectionState.NOT_CONFIGURED, "API key removed."))
        }
        return removed
    }

    fun hasApiKey(type: ProviderType): Boolean {
        return apiKeyStorage.hasKey(type.name)
    }

    fun getMaskedApiKey(type: ProviderType): String {
        val key = apiKeyStorage.getKey(type.name) ?: return ""
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}••••••••${key.takeLast(4)}"
    }

    suspend fun refreshModels(
        type: ProviderType,
        apiKeyOverride: String? = null,
        endpointOverride: String? = null
    ): List<ModelDescriptor> {
        val provider = getProvider(type) ?: return emptyList()
        val current = _configurations.value[type]
            ?: ProviderConfiguration(type, provider.descriptor.defaultModelId)
        val config = current.copy(
            customEndpoint = endpointOverride?.takeIf { it.isNotBlank() } ?: current.customEndpoint
        )
        val key = apiKeyOverride ?: apiKeyStorage.getKey(type.name)
        if (provider.descriptor.requiresApiKey && key.isNullOrBlank()) {
            updateStatus(type, ProviderConnectionStatus(type, ConnectionState.NOT_CONFIGURED, "Enter an API key before discovering models."))
            return emptyList()
        }
        updateStatus(type, ProviderConnectionStatus(type, ConnectionState.CHECKING, "Fetching models available to this API key..."))
        return try {
            val models = provider.discoverModels(config, key)
            if (models.isNotEmpty()) {
                _discoveredModels.value = _discoveredModels.value + (type to models)
                persistDiscoveredModels(type, models)
                val selected = models.firstOrNull { it.id == config.selectedModelId } ?: models.first()
                updateConfiguration(config.copy(selectedModelId = selected.id))
                updateStatus(type, ProviderConnectionStatus(type, ConnectionState.CONNECTED, "Found ${models.size} models. Selected: ${selected.id}", System.currentTimeMillis()))
            } else {
                updateStatus(type, ProviderConnectionStatus(type, ConnectionState.UNSUPPORTED_MODEL, "No compatible models were returned by this endpoint. Check the API key and endpoint."))
            }
            models
        } catch (e: Exception) {
            updateStatus(type, ProviderConnectionStatus(type, ConnectionState.NETWORK_ERROR, e.message ?: "Could not fetch models."))
            emptyList()
        }
    }

    suspend fun autoDetectProvider(rawApiKey: String, customEndpoint: String? = null): ProviderType? {
        if (rawApiKey.isBlank()) return null
        val candidates = listOf(ProviderType.GEMINI, ProviderType.OPENAI, ProviderType.OPENAI_COMPATIBLE)
        for (type in candidates) {
            val provider = getProvider(type) ?: continue
            val current = _configurations.value[type]
                ?: ProviderConfiguration(type, provider.descriptor.defaultModelId)
            val config = if (type == ProviderType.OPENAI_COMPATIBLE && !customEndpoint.isNullOrBlank()) {
                current.copy(customEndpoint = customEndpoint)
            } else current
            updateStatus(type, ProviderConnectionStatus(type, ConnectionState.CHECKING, "Checking whether this key works with ${type.displayName}..."))
            try {
                var models = provider.discoverModels(config, rawApiKey)
                if (models.isEmpty()) {
                    val test = provider.testConnection(config, rawApiKey)
                    if (test.state != ConnectionState.CONNECTED) {
                        updateStatus(type, test)
                        continue
                    }
                    models = provider.descriptor.supportedModels
                }
                if (saveApiKey(type, rawApiKey)) {
                    val selected = models.firstOrNull { it.id == config.selectedModelId } ?: models.first()
                    updateConfiguration(config.copy(selectedModelId = selected.id))
                    _discoveredModels.value = _discoveredModels.value + (type to models)
                    persistDiscoveredModels(type, models)
                    setSelectedProvider(type)
                    updateStatus(type, ProviderConnectionStatus(type, ConnectionState.CONNECTED, "Detected ${type.displayName}; found ${models.size} model(s).", System.currentTimeMillis()))
                    return type
                }
                updateStatus(type, ProviderConnectionStatus(type, ConnectionState.NETWORK_ERROR, "Provider detected, but the API key could not be saved securely."))
                return null
            } catch (e: Exception) {
                updateStatus(type, ProviderConnectionStatus(type, ConnectionState.NETWORK_ERROR, e.message ?: "Key did not validate for ${type.displayName}."))
            }
        }
        return null
    }

    suspend fun testProviderConnection(type: ProviderType): ProviderConnectionStatus {
        val provider = getProvider(type) ?: return ProviderConnectionStatus(
            type = type,
            state = ConnectionState.NOT_CONFIGURED,
            message = "Provider not found."
        )
        val config = _configurations.value[type] ?: ProviderConfiguration(type, provider.descriptor.defaultModelId)
        val key = apiKeyStorage.getKey(type.name)

        updateStatus(type, ProviderConnectionStatus(type, ConnectionState.CHECKING, "Testing connection..."))
        val status = provider.testConnection(config, key)
        updateStatus(type, status)
        return status
    }

    private fun updateStatus(type: ProviderType, status: ProviderConnectionStatus) {
        val current = _connectionStatuses.value.toMutableMap()
        current[type] = status
        _connectionStatuses.value = current
    }

    // AIBrain interface implementation
    override suspend fun query(
        prompt: String,
        privacyMode: PrivacyMode,
        imageBase64: String?
    ): AIResponse {
        // STRICT mode is useful only if it can still answer locally. Never route
        // strict-mode prompts or images through the selected cloud provider.
        val currentType = if (privacyMode == PrivacyMode.STRICT) {
            ProviderType.LOCAL_OFFLINE
        } else {
            _selectedProviderType.value
        }
        val provider = getProvider(currentType)
            ?: return AIResponse.Error("The selected AI provider is unavailable.", isOffline = true)
        val config = _configurations.value[currentType]
            ?: ProviderConfiguration(currentType, provider.descriptor.defaultModelId)
        val apiKey = if (currentType == ProviderType.LOCAL_OFFLINE) null else apiKeyStorage.getKey(currentType.name)

        if (provider.descriptor.requiresApiKey && apiKey.isNullOrBlank()) {
            return AIResponse.Error(
                message = "No API key configured for ${provider.descriptor.type.displayName}. Open AI Settings to enter your key or select Local/Offline mode.",
                isOffline = true
            )
        }

        val request = AIRequest(
            prompt = prompt,
            imageBase64 = imageBase64,
            modelId = config.selectedModelId
        )

        return when (val result = provider.query(request, config, apiKey)) {
            is AIResponseResult.Success -> AIResponse.Success(
                text = result.text,
                tokensUsed = result.tokensUsed
            )
            is AIResponseResult.Error -> AIResponse.Error(
                message = "[${result.providerType.displayName}] ${result.message}",
                isOffline = result.errorType == ProviderErrorType.LOCAL_FALLBACK || result.errorType == ProviderErrorType.MISSING_KEY
            )
        }
    }
}
