# BrahmaAI Reference Specification for Jarvis

Status: active product reference
Reference package: BrahmaAI Mobile v1.0.1 (user-provided APK)
Target: complete Android-first Jarvis personal AI assistant

## Evidence rules
- Verified in Jarvis source: behavior directly supported by current repository code.
- Indicated by BrahmaAI APK: names, resources, bundled assets, or compiled components indicate a feature family; runtime behavior is not proven.
- Requires runtime validation: behavior must be exercised in BrahmaAI and/or Jarvis on a physical device.

Do not describe a feature as complete based only on a class name, dependency, manifest declaration, successful compilation, or static APK string.

## Product target
1. Natural, context-aware conversation.
2. Voice-first interaction with a user-visible microphone lifecycle.
3. Stateful animated orb/HUD for listening, thinking, speaking, and errors.
4. Persistent, user-controllable memory and retrieval.
5. Camera, OCR, document, and separately permissioned screen perception.
6. Android actions exposed through typed, least-privilege tools.
7. Opt-in notification awareness and proactive assistance.
8. Bounded agents and reusable skills.
9. External productivity and smart-home integrations.
10. Privacy, approval, audit, and emergency-stop controls across every execution path.

## BrahmaAI APK observations
Static inspection indicated four DEX files, bundled Three.js/WebGL assets, a golden-orb HTML asset, references associated with voice interaction, memory, accessibility, notification listening, camera perception, action registration, documents, IDE integration, and smart-home integration, plus face-detection/image-processing model assets.

These observations are leads for behavioral inspection, not proof that all features are operational, enabled, or available without configuration.

## Jarvis baseline at commit b3a202eaafe53b9866fcf14c88cfaa106d5b59da
Existing source areas include AI providers and encrypted API-key storage; intent classification and local commands; tool and skill interfaces; SpeechRecognizer and TextToSpeech adapters; Compose assistant screen and orb; Room-backed memory, lexical RAG, and OKF import/export; bitmap vision and local OCR; bounded heuristic agent planner/executor; local reminder notifications; privacy modes, risk assessment, confirmation manager, audit events, and emergency stop.

Known limitations from source inspection:
- Tool and skill registries were initially empty and accepted duplicate registrations.
- Planner uses fixed phrase-based templates, not general task planning.
- Voice is command-oriented; no production wake-word service or continuous conversational session was established.
- Vision is bitmap/OCR oriented; no complete screen-perception path was established.
- Local reminders are not a notification-listener intelligence system.
- Calendar/weather adapters are placeholders; no complete accessibility or smart-home integration was established.

## Product behavior requirements

### Conversation and reasoning
- Preserve provider selection, BYOK, custom endpoints, and offline/local fallback.
- Keep conversation context bounded and user-controllable.
- Treat retrieved memories, documents, webpages, and model output as untrusted data.
- Never claim a tool action succeeded without a verified tool result.

### Voice
- Make listening status explicit.
- Background listening and wake-word activation must be opt-in, user-visible, battery-aware, and easy to stop.
- Respect Android microphone permission and foreground-service requirements.
- Cancellation and Emergency Stop must invalidate stale callbacks.

### Android actions
- All actions go through typed tools.
- Validate parameters and scope before execution.
- Check permission grants centrally.
- Require approval for consequential or destructive operations.
- Log outcomes without secrets or unnecessary personal content.

### Memory and privacy
- Retain existing Strict, Balanced, and Cloud semantics.
- Do not send memory, images, notifications, or documents to cloud unless selected privacy policy explicitly allows it.
- Support inspection, editing, export, and deletion of stored memory.

### Proactive behavior
- Every proactive feature is opt-in and configurable.
- Use scheduled work and Android lifecycle-compliant services.
- Avoid hidden surveillance, continuous location collection, or silent notification exfiltration.

## Completion definition
A feature is complete only after its behavior and privacy impact are specified; unit/integration tests pass; Android lifecycle and failure cases are tested; physical-device acceptance on the target phone passes; CI builds and packages the APK; release artifact and SHA-256 are verified; and regression plus installed-app upgrade checks pass.
