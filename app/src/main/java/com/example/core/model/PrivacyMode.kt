package com.example.core.model

enum class PrivacyMode {
    STRICT,    // Pure local processing only; zero network or cloud data transmission
    BALANCED,  // Local execution by default, cloud queries allowed only with explicit user toggle
    CLOUD      // Full cloud/AI multimodal capabilities enabled
}
