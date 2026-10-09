# JARVIS Mobile — Personal Autonomous Android OS

JARVIS Mobile is a local-first, privacy-respecting autonomous assistant for Android built with Jetpack Compose, Material 3, and Kotlin.

## Key Features

- **Bring Your Own API Key (BYOK):** Pluggable adapters for Google Gemini (`gemini-2.5-flash`, `gemini-3.5-flash`), OpenAI (`gpt-4o-mini`, `gpt-4o`), custom OpenAI-compatible endpoints (Groq, Together, vLLM, Ollama), and local offline heuristics.
- **Hardware-Backed Key Encryption:** Keys are encrypted on-device via `AndroidKeyStore` using AES-256 GCM (`AES/GCM/NoPadding`). No raw keys are ever saved in plaintext, Room, logs, or BuildConfig.
- **Strict Safety Engine:** Centralized `EmergencyStop` immediately halts all tool execution. Consequential operations require explicit user approval via `RiskEngine` and `ConfirmationManager`.
- **Bounded Agent Orchestrator:** Multi-step autonomous goal planning with a strict maximum 5-step budget.
- **Personal OS:** Local Room database managing memories, notes, tasks, and routines offline.
- **Device Control & Telemetry:** Camera flashlight torch control, volume adjustments, battery levels, date/time queries, and dialer/SMS intent dispatch.

---

## Building the Project

### Prerequisites
- JDK 17 or JDK 21
- Android SDK with platform `android-36` and build-tools installed

### Build Steps

1. Clone or extract this project.
2. Build and run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. Assemble the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. The output APK will be generated at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Automated GitHub Actions Build

A ready-to-use CI workflow is included at `.github/workflows/android-build.yml`.

Pushing this project to GitHub will automatically:
1. Build the project using Ubuntu and JDK 21.
2. Run all unit tests.
3. Assemble the debug APK.
4. Upload the generated APK as a downloadable GitHub Actions artifact (`jarvis-mobile-debug-apk`).
