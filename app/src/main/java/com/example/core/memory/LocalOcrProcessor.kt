package com.example.core.memory

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** On-device OCR for Latin and Devanagari (including Marathi/Hindi) scripts. No network call is made here. */
object LocalOcrProcessor {
    suspend fun extractText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        try {
            val latinText = runCatching { Tasks.await(latin.process(image)).text }.getOrDefault("")
            val devanagariText = runCatching { Tasks.await(devanagari.process(image)).text }.getOrDefault("")
            listOf(latinText, devanagariText).filter { it.isNotBlank() }.distinct().joinToString("\n").take(12000)
        } finally {
            latin.close()
            devanagari.close()
        }
    }
}
