# Phase 2 — Capability Dispatch and Universal BYOK

## Scope

This phase extends the Phase 1 foundation without changing the app ID, database schema, Room data, voice UI, provider key storage, privacy modes, or signing configuration.

## Capability dispatch

- Registered tools can declare a stable ID, required Android permissions, and whether explicit confirmation is required.
- Unknown/unmatched natural-language intents are offered to the registered ToolRegistry in deterministic registration order.
- Existing built-in intents keep their existing routing and cannot be shadowed by a registered extension tool.
- EmergencyStop is checked before tool lookup or execution.
- Tool confirmation is checked before execution and uses the existing ConfirmationManager pending-intent flow.
- Current Android permission grants are checked immediately before tool execution through CapabilityPermissionBroker.
- The router never launches permission dialogs. A visible, user-initiated Android UI flow must request a missing permission; the command is rejected with PERMISSION_DENIED until granted.
- Tool results and denials pass through the existing audit-event boundary.

## Universal BYOK behavior

Jarvis already stores keys using Android Keystore AES-GCM. This phase retains that mechanism and its existing provider-specific aliases.

For a new vendor that exposes an OpenAI-compatible Chat Completions API, users can configure it without a Jarvis code update:

1. Open Settings → AI Provider.
2. Select **Custom OpenAI-Compatible**.
3. Paste the vendor's API key and save it.
4. Set **Custom HTTPS Endpoint** to the vendor's API base URL (typically ending in /v1/).
5. Fetch models, or enter the exact model ID manually if model discovery is unsupported.
6. Apply the model ID, test the connection, and select the provider.

The adapter normalizes common pasted endpoint suffixes and supports common model-list response shapes. The key remains encrypted at rest and is not embedded in prompts, logs, BuildConfig, or source.

### Compatibility boundary

“Any API key” cannot literally mean every AI service protocol: a key is only an authentication credential, and different vendors may use different request/response formats, authentication headers, or non-chat APIs. Without a new app update, Jarvis can support vendors that implement the OpenAI-compatible /chat/completions protocol and compatible /models discovery (or allow a manually supplied model ID). Non-compatible APIs still need a protocol adapter or a gateway that exposes an OpenAI-compatible endpoint.

Never paste an API key into a chat message, issue, repository, or screenshot. Use the in-app masked key field. Prefer HTTPS endpoints; do not send production API keys over plain HTTP.

## Verification

- Existing CI test suite must remain green.
- Add/retain tests for registry ordering and duplicates, permission grant evaluation, endpoint normalization, model-list parsing, and key storage behavior.
- Device acceptance must verify app upgrade installation, settings persistence, an existing provider, and one custom OpenAI-compatible endpoint.
- A successful compile or CI run does not prove that a vendor endpoint accepts a user's key/model; use Test Connection on device.

## Not included yet

- Native adapters for every vendor-specific protocol.
- Automatic discovery of providers that do not expose a compatible model list.
- Background permission prompts, accessibility-service activation, notification access, or unattended sensitive actions.
- Claim of full BrahmaAI feature parity; these require separate feature-by-feature implementation and device acceptance phases.