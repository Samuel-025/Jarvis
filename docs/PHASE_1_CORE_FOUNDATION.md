# Phase 1 — Jarvis Core Foundation

Branch: feat/brahmaai-complete-assistant-foundation
Baseline: b3a202eaafe53b9866fcf14c88cfaa106d5b59da
Scope: additive, backward-compatible foundation work

## Goals
- Make tool and skill registration deterministic and thread-safe.
- Reject blank and duplicate capability IDs.
- Provide lookup, handler discovery, and unregister operations.
- Centralize permission evaluation without prompting from background code.
- Add focused regression tests.
- Preserve existing Tool and JarvisSkill implementation contracts.

## Changes in this milestone
- Tool exposes a stable default ID and multi-permission view while retaining requiredPermission.
- ToolRegistry rejects duplicate/blank IDs, supports ID lookup, all matching handlers, unregister, and clear.
- SkillRegistry rejects duplicate/blank IDs, supports ID lookup, handler discovery, unregister, and clear.
- CapabilityPermissionBroker evaluates Android permission strings through an injected checker. It does not request permissions; visible UI remains responsible for permission prompts.
- CapabilityRegistryTest covers duplicate handling, ordering, lookup, removal, permission evaluation, and no-permission capabilities.

## Explicit non-goals
- No change to main branch.
- No app ID, signing, database, provider, privacy mode, or UI changes.
- No new Android permissions or background services.
- Permission broker is not yet wired into every action execution path.
- No APK release until CI passes and device acceptance is completed.

## Validation gates
1. Run ./gradlew testDebugUnitTest.
2. Run ./gradlew assembleDebug.
3. Inspect PR workflow logs; do not rely on artifact links from old workflow runs.
4. Confirm debug signing continuity.
5. Install the new debug APK over the existing app only if signing identity matches.
6. Re-run provider, privacy, memory/OKF, voice restart, reminders, and emergency-stop regression checks.

## Next milestone
Wire permission evaluation and tool metadata into one execution boundary, then migrate existing phone actions through adapters without changing user-visible behavior.
