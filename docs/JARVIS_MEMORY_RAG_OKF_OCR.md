# Jarvis Memory, RAG, OKF and OCR

Status: first implementation milestone, version 5.5 / code 10.

## What is implemented

- **Durable transcript:** user text/voice inputs and Jarvis results are stored in Room (`conversation_messages`) and survive process death/app restart. Existing database version 1 is migrated to version 2 without destructive recreation.
- **Local RAG baseline:** deterministic token-overlap retrieval ranks saved memories, notes and prior conversation snippets. Retrieved text is bounded and labelled as untrusted reference material. This is retrieval-augmented generation, but is not yet embedding/vector RAG.
- **Privacy boundary:** Strict mode may use local retrieved context. Cloud mode is an explicit opt-in and can use retrieved context. Balanced mode does not send saved personal context unless the user enables **Include saved memory in Balanced cloud queries** in Settings. The preference defaults off.
- **OKF v0.2 export:** Personal OS provides a document-picker action that exports an actual ZIP bundle of Markdown concept documents with YAML frontmatter. The bundle includes `index.md`, memories, notes and conversation messages. Treat the exported archive as sensitive.
- **OCR:** Google ML Kit bundled Latin and Devanagari recognizers run on-device. The existing image-analysis path runs OCR before image analysis and only sends image/OCR content to the configured AI provider in Cloud privacy mode.
- **Secret redaction:** common OpenAI/OpenRouter, Gemini and Groq-style key patterns are redacted before transcript persistence. API keys configured in settings are not intentionally copied into chat history.

## OKF representation

OKF is the portable interchange/export format, not the database engine or the retrieval algorithm. Operational data remains in Room; export documents use the Open Knowledge Format v0.2 Markdown + YAML frontmatter convention. Import/round-trip support and multi-device sync are not yet implemented.

## Next upgrades, gated by benchmarks and tests

1. Replace/augment lexical retrieval with on-device embeddings and hybrid search; evaluate LiteRT-LM/EmbeddingGemma only after measuring memory, latency, thermal load and compatibility on the Galaxy A15 5G.
2. Add an import path with validation, deduplication, source provenance and a preview before writing imported OKF concepts to Room.
3. Add optional local summarization of long conversations into user-approved facts, with source links and confidence/expiry metadata. Do not silently promote every chat sentence to a permanent personal fact.
4. Add explicit controls to inspect, edit, delete and export transcripts; provide retention limits and exclude credentials, payment details and other secrets from retrieval.
5. Add OCR text review/editing before saving documents to memory, and support additional scripts only when needed and tested.
6. Add instrumented tests for database migration, process restart, OKF ZIP round-trip, OCR model availability, RAG relevance and privacy-mode routing.

## Limits

- “Remember everything” means the app can retain text turns within its local app data; it cannot remember conversations from other apps/services unless imported or connected. Android uninstall/clear-data can erase local Room data. Keep the OKF export if you need an external backup.
- Lexical RAG misses paraphrases and semantically related facts without shared words. Vector retrieval is a planned follow-up, not claimed as implemented here.
- OCR output can be inaccurate; recognized text should be reviewed for important numbers, names and dates.
- Passing CI does not prove live provider credentials, physical-device OCR accuracy or signing-certificate update compatibility.

## Research references

- Google Open Knowledge Format (canonical repository): https://github.com/GoogleCloudPlatform/open-knowledge-format
- OKF v0.2 specification snapshot: https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md
- ML Kit OCR Android guide: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- LiteRT-LM overview and on-device embedding/RAG capabilities: https://developers.google.com/edge/litert-lm/overview
- LiteRT-LM Android guide: https://developers.google.com/edge/litert-lm/android
