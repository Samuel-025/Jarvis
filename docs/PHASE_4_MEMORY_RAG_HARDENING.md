# Phase 4 — Local Memory and RAG Hardening

## Changes in this increment

- Centralized credential redaction before local memory, notes, and conversation text are persisted.
- Added redaction patterns for common OpenAI/Anthropic-style `sk-` credentials, Google AI keys, Groq keys, Hugging Face tokens, xAI keys, Bearer credentials, and explicitly labelled API/access/secret key assignments.
- Preserved the existing Room schema and migration path; this increment requires no database migration.
- Preserved local-only lexical retrieval, its bounded result count/context size, and the privacy policy that controls whether retrieved memory can be sent to a cloud model.
- Added tests for the new credential patterns and retrieval source metadata.

## Privacy and security boundaries

- Redaction is best-effort defense in depth, not a substitute for keeping API keys out of chat text.
- Custom vendor keys that do not match known formats may not be recognized unless they are placed in a labelled assignment such as `api_key=...`.
- Existing retrieval snippets remain untrusted data. The model prompt explicitly instructs the AI not to follow instructions embedded in retrieved memory.
- No embeddings, external vector database, or background cloud indexing are introduced in this increment.

## Device acceptance after CI

1. Upgrade-install the APK without uninstalling or clearing app data.
2. Confirm existing memories, notes, and conversation history remain present.
3. Save a test memory containing a fake API key and confirm the stored/displayed content shows `[REDACTED_SECRET]`.
4. Ask a question whose answer is in a saved memory and verify the relevant context is retrieved.
5. Ask an unrelated question and verify irrelevant memories are not injected.
6. Test Strict, Balanced, and Cloud privacy modes; Balanced must not send retrieved memory without explicit opt-in.
7. Export an OKF bundle and verify memory/note content remains intact.

Phase 4 remains open until CI and physical-device acceptance pass.