# Phase 3 — Voice and Assistant Experience

## Implemented in this increment

- Voice orb scales smoothly by state: idle/error, listening, and processing/speaking.
- The orb exposes a stable UI test tag for UI automation.
- Speech recognition is stopped when the app receives the Android ON_STOP lifecycle event while actively listening. This keeps microphone capture foreground-only and avoids leaving an active recognition session running after the user backgrounds Jarvis.
- Existing tap-to-talk, permission rationale, text fallback, TTS, generation guards, and Emergency Stop remain in place.
- Existing ViewModel cleanup already destroys the recognizer and shuts down TTS when the ViewModel is cleared.

## Explicitly not enabled

- No always-on microphone, wake-word listener, background recording, or automatic re-listening.
- No new microphone permission is introduced.
- No cloud voice processing is introduced; speech recognition continues to use the configured Android speech recognition service.

## Device acceptance

1. Tap microphone and verify orb expands and status shows Listening.
2. Speak a short command; verify it processes and the orb changes state.
3. Verify spoken output and return to Ready when TTS finishes.
4. Start listening, press Home or switch away from Jarvis, and verify listening stops.
5. Return to Jarvis and verify microphone remains off until explicitly tapped.
6. Deny microphone permission and verify text commands remain available.
7. Trigger Emergency Stop and verify voice capture is cancelled.

Phase 3 remains open until CI and physical-device acceptance pass.