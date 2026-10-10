package com.example.core.model

/**
 * Centralized, deterministic privacy gates shared by routing and vision.
 * Keep this policy free of Android/network dependencies so it can be unit-tested.
 */
object PrivacyRoutingPolicy {
    /** Local retrieval is permitted in Strict; Cloud is explicit; Balanced requires opt-in. */
    fun mayIncludeRetrievedMemory(mode: PrivacyMode, includeMemoryInCloud: Boolean): Boolean =
        when (mode) {
            PrivacyMode.STRICT, PrivacyMode.CLOUD -> true
            PrivacyMode.BALANCED -> includeMemoryInCloud
        }

    /** Images may be transmitted to a cloud provider only in explicitly selected Cloud mode. */
    fun mayTransmitImage(mode: PrivacyMode): Boolean = mode == PrivacyMode.CLOUD
}
