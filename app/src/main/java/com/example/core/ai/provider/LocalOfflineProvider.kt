package com.example.core.ai.provider

class LocalOfflineProvider : AIProvider {
    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        type = ProviderType.LOCAL_OFFLINE,
        supportedModels = listOf(
            ModelDescriptor(
                id = "jarvis-local-heuristic",
                displayName = "JARVIS Local Heuristic OS",
                capabilities = setOf(ProviderCapability.TEXT_GENERATION, ProviderCapability.STRUCTURED_OUTPUT)
            )
        ),
        defaultModelId = "jarvis-local-heuristic",
        requiresApiKey = false,
        allowsCustomEndpoint = false,
        defaultEndpoint = "local://device",
        apiKeyInstructionUrl = ""
    )

    override fun supportsCapability(capability: ProviderCapability, modelId: String): Boolean {
        return capability == ProviderCapability.TEXT_GENERATION || capability == ProviderCapability.STRUCTURED_OUTPUT
    }

    override suspend fun query(
        request: AIRequest,
        config: ProviderConfiguration,
        apiKey: String?
    ): AIResponseResult {
        return AIResponseResult.Success(
            text = "JARVIS Local Offline Response: Processed on-device without cloud network transmission for prompt: '${request.prompt}'. All local phone automation, personal OS memory, and device controls remain available.",
            providerType = ProviderType.LOCAL_OFFLINE,
            modelUsed = "jarvis-local-heuristic",
            tokensUsed = 0
        )
    }

    override suspend fun testConnection(
        config: ProviderConfiguration,
        apiKey: String?
    ): ProviderConnectionStatus {
        return ProviderConnectionStatus(
            type = ProviderType.LOCAL_OFFLINE,
            state = ConnectionState.CONNECTED,
            message = "Local heuristic engine active and ready (Offline mode, no key required).",
            lastCheckedTimestamp = System.currentTimeMillis()
        )
    }
}
