package com.example.core.vision

import android.graphics.Bitmap
import android.util.Base64
import com.example.core.ai.AIBrain
import com.example.core.ai.AIResponse
import com.example.core.model.PrivacyMode
import java.io.ByteArrayOutputStream

class RealVisionProvider(
    private val aiBrain: AIBrain
) : VisionProvider {

    override suspend fun analyze(bitmap: Bitmap, prompt: String): VisionResult =
        analyze(bitmap, prompt, PrivacyMode.BALANCED)

    override suspend fun analyze(
        bitmap: Bitmap,
        prompt: String,
        privacyMode: PrivacyMode
    ): VisionResult {
        if (privacyMode != PrivacyMode.CLOUD) {
            val reason = if (privacyMode == PrivacyMode.STRICT) {
                "Strict privacy mode blocks image transmission. Switch to Cloud mode to analyze a photo."
            } else {
                "Balanced mode does not send photos to cloud AI. Explicitly select Cloud mode to analyze a photo."
            }
            return VisionResult.Error(reason)
        }
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            when (val response = aiBrain.query(
                prompt = if (prompt.isBlank()) "Analyze this image and describe what you see in detail." else prompt,
                privacyMode = privacyMode,
                imageBase64 = base64String
            )) {
                is AIResponse.Success -> VisionResult.Success(
                    description = response.text,
                    detectedObjects = emptyList()
                )
                is AIResponse.Error -> VisionResult.Error(response.message)
            }
        } catch (e: Exception) {
            VisionResult.Error("Failed to encode or process image: ${e.message}")
        }
    }
}
