package com.example.core.vision

import android.graphics.Bitmap

sealed class VisionResult {
    data class Success(val description: String, val detectedObjects: List<String> = emptyList()) : VisionResult()
    data class Error(val message: String) : VisionResult()
}

interface VisionProvider {
    suspend fun analyze(bitmap: Bitmap, prompt: String = "Describe what you see"): VisionResult

    /** Privacy-aware entry point; local/test providers may keep their existing implementation. */
    suspend fun analyze(bitmap: Bitmap, prompt: String, privacyMode: com.example.core.model.PrivacyMode): VisionResult =
        analyze(bitmap, prompt)
}

class TestStubVisionProvider : VisionProvider {
    override suspend fun analyze(bitmap: Bitmap, prompt: String): VisionResult {
        return VisionResult.Success(
            description = "JARVIS Optical Sensor Analysis: Image dimensions ${bitmap.width}x${bitmap.height}. Visual feed validated under prompt: '$prompt'.",
            detectedObjects = listOf("Visual scene", "Android viewport", "Test sensor target")
        )
    }
}
