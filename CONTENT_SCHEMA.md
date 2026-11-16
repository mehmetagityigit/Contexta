# Content Schema & Authoring Guide

Curriculum packs live in `app/src/main/assets/content/` for immediate offline-ready use and mirror the Firestore curriculum collections (`contentGrammar`, `contentReading`, `contentListening`).

## Common Metadata Fields

All content items include:
- `id` (unique string, alphanumeric/`_`/`-`)
- `schemaVersion` (int `>= 1`)
- `contentVersion` (int `>= 1`)
- `level` (`B1` | `B2` | `C1` | `C2`)
- `targetExams` (`["GENERAL", "SAT", "IELTS", "AP_ACADEMIC"]`)
- `title`, `tags`, `status` (`draft` | `published` | `archived`)
- Attribution: `source`, `license`, `reviewedAt` (`YYYY-MM-DD`)

## Packs

1. **`grammar_pack.json`** (`topics[]`)
   - `orderIndex`, `titleTr`, `summaryTr`, `summaryEn`, `commonMistakesTr[]`, `examples[]` (`correct`, `incorrect`, `noteTr`, `noteEn`), and `questions[]` (`>= 3` MCQs with `explanationTr` and per-option explanations).
2. **`reading_pack.json`** (`passages[]`)
   - `levelGroup` (`B1-B2` | `C1-C2`), `genre` (`Natural Sciences` | `Social Sciences & History` | `Humanities` | `Technology & AI`), `paragraphs[]` (`id`, `text`), and `questions[]` (`>= 4` MCQs).
   - Questions with `questionType == "Command of Evidence"` MUST specify a valid `evidenceRefId` matching a paragraph `id` in the same passage.
3. **`listening_pack.json`** (`lessons[]`)
   - `contentType` (`ACADEMIC_LECTURE` | `DIALOGUE` | `SHORT_STORY`), `audioMode` (`"TTS"` or `"MEDIA3"`), `audioUri?` (required when `audioMode == "MEDIA3"`), `transcript[]` (`id`, `speaker`, `text`, `startMs`, `endMs`), and `questions[]` (`>= 4` MCQs).

## Validation & Upload CLI

- Validate schema, IDs, options, and evidence links: `node tools/validate_content.mjs`
- Dry-run upload summary: `node tools/seed_firestore_content.mjs`
- Apply upload (using `GOOGLE_APPLICATION_CREDENTIALS` or emulator): `node tools/seed_firestore_content.mjs --apply`
