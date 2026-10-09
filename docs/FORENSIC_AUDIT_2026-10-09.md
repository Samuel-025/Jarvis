# Jarvis Mobile forensic audit — 2026-10-09

## Scope and constraints

This is an Android-first, mobile-only Kotlin/Jetpack Compose app. Desktop Jarvis projects were researched for transferable ideas, not treated as implementation requirements for this phone app. Existing four-tab UI, local commands, provider selection/model discovery, encrypted API-key storage, Room-backed Personal OS data, risk confirmations, emergency stop, and bounded agent flow must remain intact.

## Defects identified and addressed

- Strict privacy mode returned an error from the provider manager while the router falsely reported a successful locally processed query. Strict mode now routes through the local offline provider, and the router displays the real result/error.
- Vision previously forced `PrivacyMode.CLOUD` regardless of the selected mode and the UI analyzed a synthetic blank bitmap while claiming to capture a sensor frame. Vision now uses Android's camera preview for a real photo, is enabled only in Cloud mode, passes the selected privacy mode to the provider, and never invents detected-object labels.
- `ERROR_NO_MATCH` and speech timeout were presented as a broken voice system. They are now recoverable idle states with a retry/text-input hint; unsupported/error states remain distinguishable.
- Text-to-speech replaced its global progress listener on every utterance and could leave voice state stuck or let stale callbacks overwrite a newer state. It now uses one listener, tracks utterance callbacks, completes callbacks on interruption/error, and uses the device locale.
- Voice command handling set IDLE before command execution completed. The premature reset was removed; generation guards prevent stale speech callbacks from overwriting newer listening/speaking state.
- Privacy mode was reset to Balanced on app recreation. It is now persisted locally.
- The Assistant status badge always showed Ready unless recognition had an error. It now reflects Listening, Processing, Speaking, Voice issue, or Ready.
- A duplicate memory search call was removed.
- Backup configuration was placeholder-only while app backup was enabled. Private shared preferences, Room databases and app files are now excluded; app backup is disabled to favor local privacy.
- The router no longer reports a vision command as successful when no photo has actually been supplied; it points users to the real photo workflow.

## Research references and transferable ideas

These are independent projects and not dependencies or proof that their features exist in this repository:

- https://github.com/talhaluxury/JARVISAssistant — native Android, visible foreground push-to-talk, whitelist-based actions, confirmation, local memory, and offline fallback.
- https://github.com/Bwarhness/jarvis-assistant — Android voice/chat, optional wake word and explicit foreground notification for background microphone use.
- https://github.com/ansaribilal14/jarvis — local-first planning, observation/action verification, and honest completed/failed/blocked reporting.
- https://github.com/PersonalJarvis/PersonalJarvis — desktop multi-agent workflows, persistent memory, routines and plugin integrations; useful as cross-platform inspiration, not a reason to add desktop code to this Android-only app.
- https://developer.android.com/develop/background-work/services/foreground-services — Android foreground-service rules; background listening must not be added without a real service, user-visible notification and appropriate permissions.

## Known gaps not silently represented as complete

- No wake-word or continuous background listening service is declared; microphone remains push-to-talk in the visible UI.
- No Accessibility service, notification listener, overlay service, or broad contacts/call/SMS permissions are declared. Do not request these unless the matching user-facing feature is implemented and Android's special-access/consent flow is provided.
- The tool and skill registries are present as architecture seams, but this audit did not claim that a populated plugin/MCP marketplace exists.
- The bounded agent is rule/planner-driven and limited to the app's registered local/phone actions; it is not a general desktop controller and does not execute arbitrary model-generated code.
- Vision uses a single preview bitmap, not a continuous camera stream or on-device object detector. Image analysis uses the selected AI provider only in Cloud mode.
- Web search, persistent conversation history, scheduled routines, wake-word operation, and cross-device/Windows/macOS control are not implemented as complete user-facing features in this repository at this audit point.

## Verification

Changes are committed to `main`; GitHub Actions is the authoritative compile/unit-test/APK check. A successful build does not replace on-device testing of voice, camera handoff, permission denial, network/provider failure, and privacy-mode behavior.
