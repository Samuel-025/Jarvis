# Jarvis Mobile — remaining-phase implementation and release checklist

Updated: 2026-10-10

This is the consolidated execution checklist for the remaining work. It distinguishes code/CI evidence from physical-device acceptance. It does not claim that unsupported local-model runtimes, Android preview APIs, or live provider credentials have been verified.

## Phase A — Durable memory and OKF
- [x] Room-backed conversation history and migration.
- [x] Local lexical hybrid retrieval with bounded context.
- [x] Memory/note edit and delete controls.
- [x] OKF v0.2 ZIP export and validated import with size/path limits.
- [x] Duplicate-aware imports; imported conversation and unknown files are not promoted to trusted memories.
- [x] Round-trip and import-validation unit tests.
- [ ] Device acceptance: export, clear only if desired, import, compare content, and verify settings/keys remain configured.

## Phase B — Privacy boundaries
- [x] Centralized pure privacy policy for retrieved context and image transmission.
- [x] Strict: local retrieved context allowed; image transmission denied.
- [x] Balanced: saved memory excluded by default; image transmission denied.
- [x] Cloud: image transmission allowed only when explicitly selected.
- [x] Unit coverage for the policy matrix.
- [ ] Integration/device acceptance: observe actual provider/network behavior for each mode; a policy unit test is not packet-level proof.

## Phase C — BYOK provider reliability
- [x] Provider/model configuration persists through SharedPreferences.
- [x] Manual model IDs and provider-specific fallback models remain supported.
- [x] Common OpenAI-compatible model-list response shapes and endpoint normalization have fixture tests.
- [x] Added negative fixtures for malformed endpoints/model schemas and duplicate IDs.
- [ ] Live opt-in smoke tests for each provider using the user's own credentials.
- [ ] Verify provider error messages on 401/403, 404, 429, timeout and 5xx without exposing keys or request headers.

## Phase D — Voice and interaction resilience
- [x] Microphone tap debounce and active-session start guard.
- [x] User verified that three rapid taps no longer produced the recognizer-busy error on the installed build.
- [ ] Device regression: permission denied/granted, no speech, background/foreground, headset/Bluetooth, and screen rotation.

## Phase E — OCR and vision
- [x] On-device Latin and Devanagari OCR is present.
- [x] Image/OCR data is routed to the AI provider only in Cloud mode.
- [ ] Device acceptance for OCR accuracy, Marathi/Hindi text, low light, rotated text, and large images.
- [ ] Review extracted text against the source image before relying on names, numbers, dates, or financial content.
- Do not automatically promote OCR text to long-term memory without explicit user confirmation.

## Phase F — Local model/runtime exploration
- [ ] LiteRT-LM or Gemini Nano remains a separate compatibility-gated prototype, not a production dependency. Device support, model download consent, storage use, RAM/thermal cost and latency must be benchmarked on the target phone before activation.
- [ ] Keep the deterministic local heuristic provider as the fallback.
- No model weights or new multi-gigabyte dependency should be bundled by this checklist.

## Phase G — Safe Android tools and integrations
- [x] Existing permission/risk/confirmation and Emergency Stop gates remain in the routing path.
- [ ] Any new capability must be registered individually, permission-gated, auditable, and tested with malformed model output.
- [ ] No unrestricted shell, background HTTP/MCP server, arbitrary file access, or silent message sending should be added.
- AppFunctions remains deferred until API/device/distribution support is confirmed.

## Release gate
A release candidate is acceptable for user testing only when:
1. GitHub Actions unit tests pass.
2. Debug APK assembly and artifact verification pass.
3. APK version/commit is identified.
4. No changes to API-key storage or provider preferences are made without a dedicated migration and tests.
5. User tests cover privacy-mode behavior and data export/import.
6. Physical-device behavior is explicitly marked as tested or untested; never infer it from CI.

## Current known limitations
- RAG is lexical/hybrid, not embedding/vector semantic search.
- Conversation memory includes only interactions Jarvis itself records or explicitly imported; it cannot automatically access other apps' chats.
- Room data can be removed by uninstall/clear-data; keep the OKF ZIP backup.
- Cloud mode sends prompts/images to the configured provider; that provider's privacy and retention terms apply.
- Unit tests and APK build do not prove provider credentials, OCR quality, Android recognizer behavior across all devices, or update-signature compatibility.
