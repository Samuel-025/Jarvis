package com.example.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class AndroidSpeechRecognizerAdapter(
    private val context: Context
) : SpeechRecognizerAdapter {

    private var speechRecognizer: SpeechRecognizer? = null
    private val isSessionActive = AtomicBoolean(false)
    private var currentOnResult: ((String) -> Unit)? = null
    private var currentOnError: ((Int, String) -> Unit)? = null

    override fun startListening(
        onResult: (String) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        cancel() // Ensure clean state before starting
        isSessionActive.set(true)
        currentOnResult = onResult
        currentOnError = onError

        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError(-1, "Speech recognition is not supported on this device")
                isSessionActive.set(false)
                return
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        if (!isSessionActive.getAndSet(false)) return
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection required for speech recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking clearly or type command."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. No sound heard."
                            else -> "Recognition error ($error)"
                        }
                        currentOnError?.invoke(error, errorMessage)
                    }

                    override fun onResults(results: Bundle?) {
                        if (!isSessionActive.getAndSet(false)) return
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrEmpty()) {
                            currentOnResult?.invoke(text)
                        } else {
                            currentOnError?.invoke(SpeechRecognizer.ERROR_NO_MATCH, "No speech recognized")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isSessionActive.set(false)
            onError(-2, "Failed to initialize speech recognition: ${e.message}")
        }
    }

    override fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    override fun cancel() {
        isSessionActive.set(false)
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        currentOnResult = null
        currentOnError = null
    }

    override fun destroy() {
        cancel()
    }
}
