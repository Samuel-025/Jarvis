package com.example.core.ai.provider

data class AIRequest(
    val prompt: String,
    val imageBase64: String? = null,
    val modelId: String? = null,
    val systemInstruction: String? = null,
    val temperature: Float? = null
)

sealed class AIResponseResult {
    data class Success(
        val text: String,
        val providerType: ProviderType,
        val modelUsed: String,
        val tokensUsed: Int = 0
    ) : AIResponseResult()

    data class Error(
        val message: String,
        val providerType: ProviderType,
        val errorType: ProviderErrorType,
        val isRetryable: Boolean = false
    ) : AIResponseResult()
}

enum class ProviderErrorType {
    MISSING_KEY,
    INVALID_KEY,
    RATE_LIMITED,
    UNSUPPORTED_CAPABILITY,
    TIMEOUT,
    NETWORK_FAILURE,
    MALFORMED_RESPONSE,
    PRIVACY_BLOCKED,
    LOCAL_FALLBACK
}

interface AIProvider {
    val descriptor: ProviderDescriptor
    fun supportsCapability(capability: ProviderCapability, modelId: String): Boolean
    suspend fun query(request: AIRequest, config: ProviderConfiguration, apiKey: String?): AIResponseResult
    suspend fun testConnection(config: ProviderConfiguration, apiKey: String?): ProviderConnectionStatus

    /** Fetch models available to this API key and endpoint. */
    suspend fun discoverModels(config: ProviderConfiguration, apiKey: String?): List<ModelDescriptor> =
        descriptor.supportedModels
}
