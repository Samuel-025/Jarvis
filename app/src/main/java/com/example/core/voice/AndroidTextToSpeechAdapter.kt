package com.example.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AndroidTextToSpeechAdapter(context: Context) : TextToSpeechAdapter {
    private val callbackLock = Any()
    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false
    private var pendingSpeech: Pair<String, () -> Unit>? = null
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()

    init {
        val engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) engineLanguageReady() else finishPending()
        }
        tts = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { complete(utteranceId) }
            override fun onError(utteranceId: String?) { complete(utteranceId) }
            override fun onError(utteranceId: String?, errorCode: Int) { complete(utteranceId) }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { complete(utteranceId) }
        })
    }

    private fun engineLanguageReady() {
        val engine = tts ?: return
        engine.language = Locale.getDefault()
        isInitialized = true
        val pending = synchronized(callbackLock) { pendingSpeech.also { pendingSpeech = null } }
        pending?.let { (text, onDone) -> speakInternal(text, onDone) }
    }

    override fun speak(text: String, onDone: () -> Unit) {
        if (text.isBlank()) { onDone(); return }
        if (!isInitialized) {
            val replaced = synchronized(callbackLock) {
                val previous = pendingSpeech
                pendingSpeech = text to onDone
                previous
            }
            replaced?.second?.invoke()
            return
        }
        speakInternal(text, onDone)
    }

    private fun speakInternal(text: String, onDone: () -> Unit) {
        val engine = tts
        if (engine == null || !isInitialized) { onDone(); return }
        val id = UUID.randomUUID().toString()
        callbacks[id] = onDone
        val result = try { engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) }
        catch (_: Exception) { TextToSpeech.ERROR }
        if (result == TextToSpeech.ERROR) complete(id)
    }

    private fun complete(id: String?) {
        if (id != null) callbacks.remove(id)?.invoke()
    }

    private fun finishPending() {
        val pending = synchronized(callbackLock) { pendingSpeech.also { pendingSpeech = null } }
        pending?.second?.invoke()
    }

    override fun stop() {
        finishPending()
        val outstanding = callbacks.values.toList()
        callbacks.clear()
        try { tts?.stop() } catch (_: Exception) {}
        outstanding.forEach { runCatching { it.invoke() } }
    }

    override fun shutdown() {
        stop()
        isInitialized = false
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
    }
}
