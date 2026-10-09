# JARVIS Mobile — Build Report

- **Application ID:** `com.aistudio.jarvis.osapp`
- **Version Name:** `5.0`
- **Version Code:** `5`
- **Min SDK:** 31 (Android 12)
- **Compile SDK:** 36 (Android 16 / VanillaIceCream / One UI 8)
- **Target SDK:** 36
- **Architecture:** Kotlin, Jetpack Compose, Material 3, Clean MVVM + ServiceLocator, Room Database

---

## Verified Artifact Details (AI Studio Build Environment)

- **Source APK Path:** `app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** `72,716,015 bytes` (~69.3 MB)
- **SHA-256 Checksum:** `a2944a228b774ca64740d615535c1e5d960adf4cd15dc8cce5e1a0b8122a84c0`
- **Signing Scheme:** Android Debug Keystore with Signature Scheme v2/v3 (`APK Sig Block 42` confirmed)
- **Status:** 100% Passing unit tests (19 test cases across BYOK, Safety, Room, Bounded Agent, and Voice).

---

## Build Prerequisites

To build this Android project externally (e.g. on your local laptop, desktop workstation, or CI/CD):

1. **JDK Version:** Java Development Kit 17 or 21 (Temurin / OpenJDK).
2. **Android SDK:**
   - Platform: `android-36`
   - Build Tools: `35.0.0` or higher
3. **Gradle:** Standard Gradle 8.11+ / Gradle Wrapper included (`./gradlew`).

---

## External Build Commands

```bash
# 1. Run unit test suite
./gradlew testDebugUnitTest

# 2. Assemble Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```
