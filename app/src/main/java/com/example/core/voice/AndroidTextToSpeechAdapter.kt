package com.example.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class AndroidTextToSpeechAdapter(
    context: Context
) : TextToSpeechAdapter {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingSpeechQueue = mutableListOf<Pair<String, () -> Unit>>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
                synchronized(pendingSpeechQueue) {
                    pendingSpeechQueue.forEach { (text, onDone) ->
                        speakInternal(text, onDone)
                    }
                    pendingSpeechQueue.clear()
                }
            }
        }
    }

    override fun speak(text: String, onDone: () -> Unit) {
        if (!isInitialized) {
            synchronized(pendingSpeechQueue) {
                pendingSpeechQueue.add(text to onDone)
            }
            return
        }
        speakInternal(text, onDone)
    }

    private fun speakInternal(text: String, onDone: () -> Unit) {
        val utteranceId = UUID.randomUUID().toString()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) onDone()
            }
            override fun onError(id: String?) {
                if (id == utteranceId) onDone()
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    override fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
