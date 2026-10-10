package com.example.core.ai.provider

enum class ProviderCapability {
    TEXT_GENERATION,
    IMAGE_UNDERSTANDING,
    STREAMING,
    STRUCTURED_OUTPUT
}

enum class ProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI"),
    OPENROUTER("OpenRouter"),
    DEEPSEEK("DeepSeek"),
    TOGETHER_AI("Together AI"),
    MISTRAL("Mistral AI"),
    OPENAI_COMPATIBLE("Custom OpenAI-Compatible"),
    LOCAL_OFFLINE("Local Heuristics (Offline)")
}

data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val capabilities: Set<ProviderCapability>,
    val defaultEndpoint: String? = null
)

data class ProviderDescriptor(
    val type: ProviderType,
    val supportedModels: List<ModelDescriptor>,
    val defaultModelId: String,
    val requiresApiKey: Boolean,
    val allowsCustomEndpoint: Boolean,
    val defaultEndpoint: String,
    val apiKeyInstructionUrl: String
)

data class ProviderConfiguration(
    val type: ProviderType,
    val selectedModelId: String,
    val customEndpoint: String? = null,
    val isEnabled: Boolean = true
)

enum class ConnectionState {
    NOT_CONFIGURED,
    CHECKING,
    CONNECTED,
    INVALID_KEY,
    RATE_LIMITED,
    NETWORK_ERROR,
    UNSUPPORTED_MODEL
}

data class ProviderConnectionStatus(
    val type: ProviderType,
    val state: ConnectionState,
    val message: String,
    val lastCheckedTimestamp: Long = 0L
)
