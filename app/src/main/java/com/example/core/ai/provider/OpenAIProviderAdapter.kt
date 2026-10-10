package com.example.core.ai.provider

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenAIProviderAdapter(
    override val descriptor: ProviderDescriptor = defaultDescriptor()
) : AIProvider {

    companion object {
        internal fun defaultModelFor(type: ProviderType): String = when (type) {
            ProviderType.OPENROUTER -> "openai/gpt-4o-mini"
            ProviderType.DEEPSEEK -> "deepseek-chat"
            ProviderType.TOGETHER_AI -> "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo"
            ProviderType.MISTRAL -> "mistral-small-latest"
            else -> "gpt-4o-mini"
        }

        private fun defaultModelsFor(type: ProviderType): List<ModelDescriptor> {
            val id = defaultModelFor(type)
            val label = when (type) {
                ProviderType.OPENROUTER -> "OpenRouter default"
                ProviderType.DEEPSEEK -> "DeepSeek default"
                ProviderType.TOGETHER_AI -> "Together AI default"
                ProviderType.MISTRAL -> "Mistral default"
                ProviderType.OPENAI_COMPATIBLE -> "Configured provider default"
                else -> "GPT-4o mini (Fast & Vision)"
            }
            val capabilities = buildSet {
                add(ProviderCapability.TEXT_GENERATION)
                add(ProviderCapability.STREAMING)
                if (type == ProviderType.OPENAI || type == ProviderType.OPENROUTER) {
                    add(ProviderCapability.IMAGE_UNDERSTANDING)
                    add(ProviderCapability.STRUCTURED_OUTPUT)
                }
            }
            return listOf(ModelDescriptor(id = id, displayName = label, capabilities = capabilities))
        }

        internal fun normalizeBaseUrl(rawEndpoint: String): String {
            var endpoint = rawEndpoint.trim().substringBefore('?').substringBefore('#').trimEnd('/')
            val suffixes = listOf("/chat/completions", "/completions", "/models")
            for (suffix in suffixes) {
                if (endpoint.endsWith(suffix, ignoreCase = true)) {
                    endpoint = endpoint.dropLast(suffix.length).trimEnd('/')
                    break
                }
            }
            require(endpoint.startsWith("https://", ignoreCase = true) ||
                endpoint.startsWith("http://", ignoreCase = true)) {
                "Endpoint must start with http:// or https://."
            }
            return endpoint + "/"
        }

        internal fun parseModelsResponse(body: String): List<ModelDescriptor> {
            val root = org.json.JSONTokener(body).nextValue()
            val data = when (root) {
                is JSONArray -> root
                is JSONObject -> root.optJSONArray("data")
                    ?: root.optJSONArray("models")
                    ?: root.optJSONArray("items")
                else -> null
            } ?: throw IOException("The endpoint did not return a recognized model list (expected data, models, items, or a JSON array).")

            return buildList {
                for (index in 0 until data.length()) {
                    val item = data.optJSONObject(index) ?: continue
                    val id = sequenceOf("id", "model", "name")
                        .map { item.optString(it, "").trim() }
                        .firstOrNull { it.isNotBlank() && it != "null" }
                        ?: continue
                    val lower = id.lowercase()
                    val capabilities = buildSet {
                        add(ProviderCapability.TEXT_GENERATION)
                        if (listOf("vision", "4o", "llava", "pixtral", "gemini", "claude-3", "gpt-4.1").any(lower::contains)) {
                            add(ProviderCapability.IMAGE_UNDERSTANDING)
                        }
                        if (listOf("gpt", "gemini", "claude").any(lower::contains)) {
                            add(ProviderCapability.STRUCTURED_OUTPUT)
                        }
                        add(ProviderCapability.STREAMING)
                    }
                    val displayName = item.optString("name", id).takeIf { it.isNotBlank() } ?: id
                    add(ModelDescriptor(id = id, displayName = displayName, capabilities = capabilities))
                }
            }.distinctBy { it.id }.sortedBy { it.id }
        }

        fun defaultDescriptor(
            type: ProviderType = ProviderType.OPENAI,
            defaultEndpoint: String = "https://api.openai.com/v1/"
        ) = ProviderDescriptor(
            type = type,
            supportedModels = defaultModelsFor(type),
            defaultModelId = defaultModelFor(type),
            requiresApiKey = true,
            allowsCustomEndpoint = type == ProviderType.OPENAI_COMPATIBLE,
            defaultEndpoint = defaultEndpoint,
            apiKeyInstructionUrl = "https://platform.openai.com/api-keys"
        )
    }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun supportsCapability(capability: ProviderCapability, modelId: String): Boolean {
        val model = descriptor.supportedModels.firstOrNull { it.id == modelId }
        if (model != null) return model.capabilities.contains(capability)
        val id = modelId.lowercase()
        return when (capability) {
            ProviderCapability.TEXT_GENERATION -> true
            ProviderCapability.IMAGE_UNDERSTANDING -> listOf("vision", "4o", "llava", "pixtral", "gemini", "claude-3").any(id::contains)
            ProviderCapability.STRUCTURED_OUTPUT -> listOf("gpt", "gemini", "claude").any(id::contains)
            ProviderCapability.STREAMING -> true
        }
    }

    override suspend fun discoverModels(
        config: ProviderConfiguration,
        apiKey: String?
    ): List<ModelDescriptor> = withContext(Dispatchers.IO) {
        require(!apiKey.isNullOrBlank()) { "Enter an API key first." }
        val baseUrl = normalizeBaseUrl(config.customEndpoint?.takeIf { it.isNotBlank() } ?: descriptor.defaultEndpoint)
        val request = Request.Builder()
            .url("${baseUrl}models")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Model discovery failed (HTTP ${response.code}): ${parseErrorMessage(body)}")
            }
            parseModelsResponse(body).also { models ->
                if (models.isEmpty()) throw IOException("The endpoint returned no usable model identifiers.")
            }
        }
    }

    override suspend fun query(
        request: AIRequest,
        config: ProviderConfiguration,
        apiKey: String?
    ): AIResponseResult = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext AIResponseResult.Error(
                message = "Missing API key for ${descriptor.type.displayName}. Please configure it in AI Settings.",
                providerType = descriptor.type,
                errorType = ProviderErrorType.MISSING_KEY
            )
        }

        val baseUrl = normalizeBaseUrl(config.customEndpoint?.takeIf { it.isNotBlank() } ?: descriptor.defaultEndpoint)
        val targetUrl = "${baseUrl}chat/completions"
        val modelId = config.selectedModelId.ifBlank { descriptor.defaultModelId }

        try {
            val root = JSONObject()
            root.put("model", modelId)

            val messages = JSONArray()
            val userMsg = JSONObject()
            userMsg.put("role", "user")

            if (request.imageBase64 != null) {
                if (!supportsCapability(ProviderCapability.IMAGE_UNDERSTANDING, modelId)) {
                    return@withContext AIResponseResult.Error(
                        message = "Selected model '$modelId' does not support image input.",
                        providerType = descriptor.type,
                        errorType = ProviderErrorType.UNSUPPORTED_CAPABILITY
                    )
                }
                val contentArray = JSONArray()
                val textObj = JSONObject().put("type", "text").put("text", request.prompt)
                val imgObj = JSONObject().put("type", "image_url")
                val imgUrlObj = JSONObject().put("url", "data:image/jpeg;base64,${request.imageBase64}")
                imgObj.put("image_url", imgUrlObj)
                contentArray.put(textObj)
                contentArray.put(imgObj)
                userMsg.put("content", contentArray)
            } else {
                userMsg.put("content", request.prompt)
            }
            messages.put(userMsg)
            root.put("messages", messages)

            val body = root.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(targetUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .apply {
                    if (descriptor.type == ProviderType.OPENROUTER) {
                        addHeader("X-OpenRouter-Title", "Jarvis Mobile")
                    }
                }
                .post(body)
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val code = response.code

                if (!response.isSuccessful) {
                    val errorType = when (code) {
                        401, 403 -> ProviderErrorType.INVALID_KEY
                        429 -> ProviderErrorType.RATE_LIMITED
                        404 -> ProviderErrorType.UNSUPPORTED_CAPABILITY
                        else -> ProviderErrorType.NETWORK_FAILURE
                    }
                    return@withContext AIResponseResult.Error(
                        message = "${descriptor.type.displayName} returned HTTP $code: ${parseErrorMessage(responseBody)}",
                        providerType = descriptor.type,
                        errorType = errorType,
                        isRetryable = code == 429 || code >= 500
                    )
                }

                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                val firstChoice = choices?.optJSONObject(0)
                val messageObj = firstChoice?.optJSONObject("message")
                val content = messageObj?.optString("content")

                if (!content.isNullOrBlank()) {
                    AIResponseResult.Success(
                        text = content.trim(),
                        providerType = descriptor.type,
                        modelUsed = modelId
                    )
                } else {
                    AIResponseResult.Error(
                        message = "Malformed or empty response payload from endpoint.",
                        providerType = descriptor.type,
                        errorType = ProviderErrorType.MALFORMED_RESPONSE
                    )
                }
            }
        } catch (e: IOException) {
            AIResponseResult.Error(
                message = "Network error connecting to ${descriptor.type.displayName}: ${e.message}",
                providerType = descriptor.type,
                errorType = ProviderErrorType.NETWORK_FAILURE,
                isRetryable = true
            )
        } catch (e: Exception) {
            AIResponseResult.Error(
                message = "Failed to process request: ${e.message}",
                providerType = descriptor.type,
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
                type = descriptor.type,
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
                type = descriptor.type,
                state = ConnectionState.CONNECTED,
                message = "Connection successful. Model: ${config.selectedModelId}",
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
                    type = descriptor.type,
                    state = state,
                    message = result.message,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
            }
        }
    }

    private fun parseErrorMessage(jsonBody: String): String {
        return try {
            val obj = JSONObject(jsonBody)
            val err = obj.optJSONObject("error")
            err?.optString("message") ?: jsonBody
        } catch (_: Exception) {
            jsonBody
        }
    }
}
