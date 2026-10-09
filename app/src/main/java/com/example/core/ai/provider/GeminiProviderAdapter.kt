package com.example.core.ai.provider

import com.example.core.ai.CandidateDto
import com.example.core.ai.ContentDto
import com.example.core.ai.GeminiApiClient
import com.example.core.ai.GeminiApiService
import com.example.core.ai.GeminiRequestDto
import com.example.core.ai.InlineDataDto
import com.example.core.ai.PartDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class GeminiProviderAdapter(
    private val apiService: GeminiApiService = GeminiApiClient.apiService
) : AIProvider {

    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        type = ProviderType.GEMINI,
        supportedModels = listOf(
            ModelDescriptor(
                id = "gemini-2.5-flash",
                displayName = "Gemini 2.5 Flash (Multimodal & Fast)",
                capabilities = setOf(
                    ProviderCapability.TEXT_GENERATION,
                    ProviderCapability.IMAGE_UNDERSTANDING,
                    ProviderCapability.STRUCTURED_OUTPUT
                )
            ),
            ModelDescriptor(
                id = "gemini-3.5-flash",
                displayName = "Gemini 3.5 Flash (Reasoning & Text)",
                capabilities = setOf(
                    ProviderCapability.TEXT_GENERATION,
                    ProviderCapability.STRUCTURED_OUTPUT
                )
            )
        ),
        defaultModelId = "gemini-2.5-flash",
        requiresApiKey = true,
        allowsCustomEndpoint = false,
        defaultEndpoint = "https://generativelanguage.googleapis.com/",
        apiKeyInstructionUrl = "https://aistudio.google.com/app/apikey"
    )

    override fun supportsCapability(capability: ProviderCapability, modelId: String): Boolean {
        val model = descriptor.supportedModels.firstOrNull { it.id == modelId }
        if (model != null) return model.capabilities.contains(capability)
        val id = modelId.lowercase()
        return when (capability) {
            ProviderCapability.TEXT_GENERATION -> true
            ProviderCapability.IMAGE_UNDERSTANDING -> id.contains("flash") || id.contains("pro") || id.contains("vision")
            ProviderCapability.STRUCTURED_OUTPUT -> id.contains("flash") || id.contains("pro")
            ProviderCapability.STREAMING -> false
        }
    }

    override suspend fun discoverModels(
        config: ProviderConfiguration,
        apiKey: String?
    ): List<ModelDescriptor> = withContext(Dispatchers.IO) {
        require(!apiKey.isNullOrBlank()) { "Enter a Gemini API key first." }
        val response = apiService.listModels(apiKey)
        response.models.orEmpty()
            .filter { model -> model.supportedGenerationMethods.orEmpty().contains("generateContent") }
            .mapNotNull { model ->
                val id = model.name.substringAfterLast("/")
                if (id.isBlank()) null else {
                    val lower = id.lowercase()
                    val capabilities = buildSet {
                        add(ProviderCapability.TEXT_GENERATION)
                        if (lower.contains("flash") || lower.contains("pro") || lower.contains("vision")) {
                            add(ProviderCapability.IMAGE_UNDERSTANDING)
                            add(ProviderCapability.STRUCTURED_OUTPUT)
                        }
                    }
                    ModelDescriptor(
                        id = id,
                        displayName = model.displayName?.takeIf { it.isNotBlank() } ?: id,
                        capabilities = capabilities
                    )
                }
            }
            .distinctBy { it.id }
            .sortedBy { it.id }
    }

    override suspend fun query(
        request: AIRequest,
        config: ProviderConfiguration,
        apiKey: String?
    ): AIResponseResult = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext AIResponseResult.Error(
                message = "Missing Gemini API key. Please configure your key in AI Settings.",
                providerType = ProviderType.GEMINI,
                errorType = ProviderErrorType.MISSING_KEY
            )
        }

        val modelId = config.selectedModelId.ifBlank { descriptor.defaultModelId }

        if (request.imageBase64 != null && !supportsCapability(ProviderCapability.IMAGE_UNDERSTANDING, modelId)) {
            return@withContext AIResponseResult.Error(
                message = "Model '$modelId' does not support image input.",
                providerType = ProviderType.GEMINI,
                errorType = ProviderErrorType.UNSUPPORTED_CAPABILITY
            )
        }

        try {
            val parts = mutableListOf<PartDto>()
            parts.add(PartDto(text = request.prompt))
            if (request.imageBase64 != null) {
                parts.add(PartDto(inlineData = InlineDataDto(mimeType = "image/jpeg", data = request.imageBase64)))
            }

            val requestDto = GeminiRequestDto(
                contents = listOf(ContentDto(parts = parts))
            )

            val response = apiService.generateContent(
                endpoint = "v1beta/models/$modelId:generateContent",
                apiKey = apiKey,
                request = requestDto
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!text.isNullOrBlank()) {
                AIResponseResult.Success(
                    text = text.trim(),
                    providerType = ProviderType.GEMINI,
                    modelUsed = modelId
                )
            } else {
                AIResponseResult.Error(
                    message = "Empty response received from Gemini.",
                    providerType = ProviderType.GEMINI,
                    errorType = ProviderErrorType.MALFORMED_RESPONSE
                )
            }
        } catch (e: HttpException) {
            val code = e.code()
            val errorType = when (code) {
                400, 401, 403 -> ProviderErrorType.INVALID_KEY
                429 -> ProviderErrorType.RATE_LIMITED
                else -> ProviderErrorType.NETWORK_FAILURE
            }
            AIResponseResult.Error(
                message = "Gemini API error (HTTP $code): ${e.message()}",
                providerType = ProviderType.GEMINI,
                errorType = errorType,
                isRetryable = code == 429 || code >= 500
            )
        } catch (e: IOException) {
            AIResponseResult.Error(
                message = "Network connection failure reaching Gemini endpoint: ${e.message}",
                providerType = ProviderType.GEMINI,
                errorType = ProviderErrorType.NETWORK_FAILURE,
                isRetryable = true
            )
        } catch (e: Exception) {
            AIResponseResult.Error(
                message = "Unexpected Gemini error: ${e.message}",
                providerType = ProviderType.GEMINI,
                errorType = ProviderErrorType.NETWORK_FAILURE
            )
        }
    }

    override suspend fun testConnection(
        config: ProviderConfiguration,
        apiKey: String?
    ): ProviderConnectionStatus = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext ProviderConnectionStatus(
                type = ProviderType.GEMINI,
                state = ConnectionState.NOT_CONFIGURED,
                message = "No API key configured."
            )
        }
        val result = query(
            request = AIRequest("Respond with the single word: OK"),
            config = config,
            apiKey = apiKey
        )
        return@withContext when (result) {
            is AIResponseResult.Success -> ProviderConnectionStatus(
                type = ProviderType.GEMINI,
                state = ConnectionState.CONNECTED,
                message = "Connection successful. Verified model: ${config.selectedModelId}",
                lastCheckedTimestamp = System.currentTimeMillis()
            )
            is AIResponseResult.Error -> {
                val state = when (result.errorType) {
                    ProviderErrorType.INVALID_KEY, ProviderErrorType.MISSING_KEY -> ConnectionState.INVALID_KEY
                    ProviderErrorType.RATE_LIMITED -> ConnectionState.RATE_LIMITED
                    ProviderErrorType.UNSUPPORTED_CAPABILITY -> ConnectionState.UNSUPPORTED_MODEL
                    else -> ConnectionState.NETWORK_ERROR
                }
                ProviderConnectionStatus(
                    type = ProviderType.GEMINI,
                    state = state,
                    message = result.message,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
            }
        }
    }
}
