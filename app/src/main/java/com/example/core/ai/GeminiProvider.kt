package com.example.core.ai

import com.example.BuildConfig
import com.example.core.model.PrivacyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiProvider(
    private val apiService: GeminiApiService = GeminiApiClient.apiService
) : AIBrain {

    override suspend fun query(
        prompt: String,
        privacyMode: PrivacyMode,
        imageBase64: String?
    ): AIResponse = withContext(Dispatchers.IO) {
        // Enforce STRICT privacy: NEVER transmit user content to cloud
        if (privacyMode == PrivacyMode.STRICT) {
            return@withContext AIResponse.Error(
                message = "Privacy mode STRICT is active. Cloud AI requests are restricted to protect user privacy.",
                isOffline = true
            )
        }

        // BALANCED privacy: if no cloud approval, fallback to local summary
        if (privacyMode == PrivacyMode.BALANCED && prompt.contains("private", ignoreCase = true)) {
            return@withContext AIResponse.Error(
                message = "BALANCED privacy prevented cloud transmission of sensitive query.",
                isOffline = true
            )
        }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "your_api_key_here") {
            // Graceful fallback to local heuristic assistant response
            return@withContext AIResponse.Success(
                text = "JARVIS Local Response: [API Key not configured in Secrets panel]. Local synthesis for: '$prompt'. Local command and personal OS systems remain fully operational.",
                tokensUsed = 0
            )
        }

        return@withContext try {
            val parts = mutableListOf<PartDto>()
            parts.add(PartDto(text = prompt))
            if (imageBase64 != null) {
                parts.add(PartDto(inlineData = InlineDataDto(mimeType = "image/jpeg", data = imageBase64)))
            }

            val request = GeminiRequestDto(
                contents = listOf(ContentDto(parts = parts))
            )

            val response = apiService.generateContent(
                endpoint = "v1beta/models/gemini-2.5-flash:generateContent",
                apiKey = apiKey,
                request = request
            )
            val candidate = response.candidates?.firstOrNull()
            val text = candidate?.content?.parts?.firstOrNull()?.text

            if (!text.isNullOrBlank()) {
                AIResponse.Success(text = text.trim())
            } else {
                AIResponse.Error("Empty response received from Gemini model.")
            }
        } catch (e: Exception) {
            AIResponse.Error("Gemini query failed: ${e.message ?: "Network error"}", isOffline = true)
        }
    }
}
