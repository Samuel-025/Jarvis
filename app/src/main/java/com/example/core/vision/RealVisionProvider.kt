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

    override suspend fun analyze(bitmap: Bitmap, prompt: String): VisionResult {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            when (val response = aiBrain.query(
                prompt = if (prompt.isBlank()) "Analyze this image and describe what you see in detail." else prompt,
                privacyMode = PrivacyMode.CLOUD,
                imageBase64 = base64String
            )) {
                is AIResponse.Success -> VisionResult.Success(
                    description = response.text,
                    detectedObjects = listOf("Visual scene element")
                )
                is AIResponse.Error -> VisionResult.Error(response.message)
            }
        } catch (e: Exception) {
            VisionResult.Error("Failed to encode or process image: ${e.message}")
        }
    }
}
