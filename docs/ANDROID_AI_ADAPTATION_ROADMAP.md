# Android AI Adaptation Roadmap for Jarvis Mobile

Last reviewed: 2026-10-10

This document records candidate technologies beyond assistant-specific repositories. It separates safe architectural reuse from integrations that need device compatibility, permissions, licensing, and performance checks.

## Guiding constraints
- Preserve the existing native Jetpack Compose UI and provider settings.
- Never log or commit API keys. Keep keys in the existing secure storage implementation.
- Use least-privilege tools: register a capability only when its permission is granted and the user has enabled it.
- Require explicit confirmation for sending messages, changing sensitive settings, deleting data, installing apps, sharing personal records, or making purchases.
- Treat model output as untrusted input. Validate tool names, parameters, URLs, file paths, and action scope before execution.
- Cloud, on-device, and offline modes must remain visibly distinct. Do not silently upload private data.
- Do not claim a feature is available until it has passed unit tests and device-level tests on the target hardware.

## Highest-value candidates

### 1. LiteRT-LM — local language models and tool calling
Source: https://github.com/google-ai-edge/LiteRT-LM
Kotlin guide: https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
Samples: https://github.com/google-ai-edge/litert-samples
License: Apache 2.0 for LiteRT-LM.

Why it fits: provides a maintained Kotlin API for local language models, streaming responses, multimodal support, and tool-use workflows. It could eventually replace or augment the current heuristic-only local provider with an actual downloadable local model.

Plan: prototype in an isolated LocalModelProvider behind the existing AIProvider interface. Download model files only after clear consent, verify checksums, show storage requirements, load/unload off the UI thread, handle low memory, and expose a small-model option. Keep the existing local heuristic provider as the reliable fallback.

Device caveat: hardware acceleration and latency vary by chipset, RAM, model quantization, and driver. Benchmark on the actual Samsung Galaxy A15 5G before making a model the default. Do not bundle multi-gigabyte weights in the APK.

### 2. Android ML Kit GenAI / Gemini Nano — local task-specific intelligence
Docs: https://developer.android.com/ai/gemini-nano
Prompt API setup: https://developer.android.com/agents/skills/device-ai/ml-kit-genai-prompt-api/references/get-started

Why it fits: task-specific on-device summarization, classification, extraction, rewriting, and selected image/text tasks without sending inputs to a cloud provider.

Plan: first add capability detection and a small experimental adapter, not a hard dependency in the main chat path. If the device/model is unavailable, route to the configured provider only when privacy settings permit; otherwise use deterministic local behavior and explain the limitation.

Device caveat: model and API availability depend on device, OS, distribution, and provider support. A dependency compiling is not proof the user's phone supports inference.

### 3. Android AppFunctions — system-level agent interoperability
Docs: https://developer.android.com/ai/appfunctions
Implementation article: https://android-developers.googleblog.com/2026/07/build-intelligent-android-apps-appfunctions.html

Why it fits: exposes carefully selected app operations as typed, discoverable functions for Android intelligence features and agents. Jarvis could eventually expose safe functions such as getBatteryStatus, searchNotes, createDraftTask, and getNextReminder.

Plan: defer implementation until the API and distribution constraints are confirmed for the target device and release channel. Start by writing clear typed domain functions and tests so they can later be wrapped in AppFunctions. Never expose unrestricted shell, arbitrary file access, secrets, or unconfirmed destructive actions.

Caveat: AppFunctions is documented as experimental/preview and access to system agents can be restricted. It is not a replacement for Jarvis's own UI or internal tool registry.

### 4. Droid-MCP — modular Android capability tools
Source: https://github.com/stixez/droid-mcp
License: Apache 2.0 according to the repository.

Why it fits: demonstrates typed Android tools for device state, apps, calendar, camera, audio, files, notifications, and more. It also documents permission gating, confirmation, structured tool results, and local MCP use.

Plan: borrow the module-by-capability design and security review checklist first. Do not add the all-in-one dependency immediately. Evaluate individual modules and transitive dependencies in a separate branch; start with read-only battery, time, and app-launching capabilities already covered by Jarvis. Only add calendar, contacts, files, notifications, or accessibility when the user explicitly enables them and each is independently reviewed.

Security caveat: an on-device HTTP/MCP server expands the attack surface. Jarvis should not expose a network server by default. If remote pairing is ever added, require strong authentication, explicit user opt-in, safe network binding, request limits, session controls, and a documented threat model.

### 5. Firebase AI Logic — managed cloud and hybrid inference
Docs: https://firebase.google.com/docs/ai-logic

Why it fits: a managed route for Gemini-backed cloud features and Firebase integrations. Jarvis already includes Firebase dependencies, but that alone does not establish that AI Logic is configured or usable.

Plan: compare its value against the current direct BYOK provider architecture before adopting. Preserve user choice of provider and make any server-mediated requests explicit. Never migrate or proxy personal API keys without a clear security and cost model.

### 6. AWS Sample Mobile AI Assistant — cross-provider product patterns
Source: https://github.com/aws-samples/sample-mobile-ai-assistant
License: MIT No Attribution according to its repository.

Why it fits: demonstrates provider/model settings, streamed chat, voice conversations, and tool-rich AI workflows in a mobile app.

Plan: use as a behavioral reference for model selection, streaming, and capability boundaries. Do not transplant its React Native architecture or AWS server setup into the native Kotlin app without a concrete need.

### 7. Aimybox — voice pipeline modularity
Sources: https://github.com/just-ai/aimybox-android-sdk and https://github.com/just-ai/aimybox-android-assistant
License: Apache 2.0.

Why it fits: separates speech recognition, text-to-speech, NLU, and skills behind interchangeable components.

Plan: use as an architectural reference for speech-engine abstraction and lifecycle tests. Avoid importing a second complete voice stack until the existing microphone restart regression is fixed and measured.

## Provider adapter acceptance criteria
1. Normalize base URLs with /v1, /models, /chat/completions, query strings, and trailing slashes.
2. Parse the provider's documented model-list schema and reject empty or malformed results with actionable errors.
3. Use provider-appropriate fallback models, and permit manual model-ID entry when discovery is unavailable.
4. Verify chat completion response parsing and handle non-string or empty content without crashing.
5. Map 401/403, 404, 429, timeouts, and server errors to useful messages; avoid printing credentials or full request headers.
6. Test endpoint normalization and response parsing with fixtures, then perform opt-in live smoke tests using the user's own keys.
7. Keep the provider's selected model and endpoint stable across process death and app upgrades.
8. Confirm privacy mode before any image, note, or memory is sent to a cloud endpoint.

## Regression and release gates
- Unit tests: endpoint normalization, model schema fixtures, key storage, selected provider persistence, and privacy routing.
- Voice tests: tap → listen → result → tap again; no-speech timeout; permission denial; app background/foreground; recognizer busy; rapid repeated taps.
- Tool safety: malformed model-generated arguments never invoke Android actions; high-risk actions require confirmation.
- Device test: install over the currently installed version with matching signing certificate; verify the provider selection and keys remain intact.
- CI: tests and APK assembly must pass. A successful build is not a substitute for live provider or physical-device testing.

## Recommended implementation order
1. Harden provider discovery and add deterministic fixture tests.
2. Fix and device-test the speech recognizer lifecycle.
3. Add manual model-ID fallback and improve actionable provider errors.
4. Add an optional local-model prototype using LiteRT-LM behind the existing provider interface.
5. Evaluate ML Kit GenAI capability detection for selected private/offline tasks.
6. Design a least-privilege tool registry informed by Droid-MCP, without adopting broad permissions.
7. Reassess AppFunctions when target-device and distribution support are confirmed.
8. Only then consider MCP server exposure, notifications, accessibility, or background automation.

## Evidence and limitations
This is a candidate-resource review, not a claim that every listed project has been built into Jarvis. Repository documentation and licenses should be rechecked at the time code is copied or dependencies are added. Actual model availability, performance, and Android API behavior must be tested on the target device.