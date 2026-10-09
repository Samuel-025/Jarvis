package com.example.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class AndroidSpeechRecognizerAdapter(
    private val context: Context
) : SpeechRecognizerAdapter {

    @Volatile private var speechRecognizer: SpeechRecognizer? = null
    private val sessionCounter = AtomicLong(0L)
    @Volatile private var activeSessionId = 0L
    private val isSessionActive = AtomicBoolean(false)

    override fun startListening(onResult: (String) -> Unit, onError: (Int, String) -> Unit) {
        cancel()
        val sessionId = sessionCounter.incrementAndGet()
        activeSessionId = sessionId
        isSessionActive.set(true)

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            finishWithoutRecognizer(sessionId)
            onError(-1, "Speech recognition is not supported on this device")
            return
        }

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    if (!claimSession(sessionId)) return
                    releaseRecognizer(sessionId, recognizer)
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer reset. Tap the microphone to try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection required for speech recognition"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking clearly or type command."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy. Tap the microphone to try again."
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. No sound heard."
                        else -> "Recognition error ($error)"
                    }
                    onError(error, message)
                }

                override fun onResults(results: Bundle?) {
                    if (!claimSession(sessionId)) return
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    releaseRecognizer(sessionId, recognizer)
                    if (!text.isNullOrEmpty()) onResult(text)
                    else onError(SpeechRecognizer.ERROR_NO_MATCH, "No speech recognized")
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            if (claimSession(sessionId)) {
                speechRecognizer?.let { releaseRecognizer(sessionId, it) }
                onError(-2, "Failed to initialize speech recognition: ${e.message ?: "unknown error"}")
            }
        }
    }

    private fun claimSession(sessionId: Long): Boolean =
        activeSessionId == sessionId && isSessionActive.compareAndSet(true, false)

    private fun finishWithoutRecognizer(sessionId: Long) {
        if (activeSessionId == sessionId) isSessionActive.set(false)
    }

    private fun releaseRecognizer(sessionId: Long, recognizer: SpeechRecognizer) {
        if (activeSessionId != sessionId) return
        if (speechRecognizer === recognizer) speechRecognizer = null
        try { recognizer.cancel() } catch (_: Exception) {}
        try { recognizer.destroy() } catch (_: Exception) {}
    }

    override fun stopListening() {
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
    }

    override fun cancel() {
        activeSessionId = sessionCounter.incrementAndGet()
        isSessionActive.set(false)
        val previous = speechRecognizer
        speechRecognizer = null
        if (previous != null) {
            try { previous.cancel() } catch (_: Exception) {}
            try { previous.destroy() } catch (_: Exception) {}
        }
    }

    override fun destroy() = cancel()
}
