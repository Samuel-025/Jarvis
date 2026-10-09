package com.example.core.voice

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

interface SpeechRecognizerAdapter {
    fun startListening(onResult: (String) -> Unit, onError: (Int, String) -> Unit)
    fun stopListening()
    fun cancel()
    fun destroy()
}

interface TextToSpeechAdapter {
    fun speak(text: String, onDone: () -> Unit = {})
    fun stop()
    fun shutdown()
}
