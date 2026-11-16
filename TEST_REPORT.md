# Contexta Test & Verification Report

This report distinguishes clearly between **locally executed & verified** checks and **physical-device checks pending hardware access** (since this environment has no attached physical device or ADB runtime).

## 1. Executed & Passed Verification

| Suite / Check | Scope Covered | Result |
| :--- | :--- | :--- |
| **`compile_applet` (`assembleDebug`)** | Full Android Kotlin + Jetpack Compose + Material 3 + Media3 + Firebase build | **PASSED** |
| **`node tools/validate_content.mjs`** | Validates `grammar_pack.json` (15 topics / 45 Qs), `reading_pack.json` (8 passages / 32 Qs), `listening_pack.json` (3 lessons / 12 Qs), unique IDs, CEFR levels, `explanationTr`, and `Command of Evidence` `evidenceRefId` links | **PASSED** |
| **`FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`** | 5 JS security rules tests: unauthenticated denial, owner CRUD, cross-user isolation, ghost-field/shadow-update rejection, published-only curriculum reads | **5/5 PASSED** |
| **`gradle :app:testDebugUnitTest --tests '*Rule*'`** (`VocabularyRepositoryRuleTest`) | 4 Robolectric + Firebase Auth/Firestore emulator tests (`127.0.0.1:9099` & `127.0.0.1:8085`): normalized vocabulary deduplication, idempotent `submitFlashcardReview`, cross-user `PERMISSION_DENIED`, unauthenticated `observeVocabulary` `PERMISSION_DENIED` | **4/4 PASSED** |
| **`gradle :app:testDebugUnitTest` (Domain & Bundle Unit Tests)** | 14 tests across `SpacedRepetitionEngineTest` (4), `PracticeScoringEngineTest` (2), `ContentValidatorTest` (4), `UserDataTransferManagerTest` (2), `ExampleRobolectricTest` (1), `ExampleUnitTest` (1) | **14/14 PASSED** |
| **Security & Static Grep Gate** | Zero occurrences of `signInAnonymously`, `signInWithEmailAndPassword`, `.apps.googleusercontent.com`, parameterless `Firebase.firestore`, or `FirebaseFirestore.getInstance()` in `app/src/main` | **PASSED (0 matches)** |
| **Firebase Provisioning & Rules Deploy** | `ProvisionFirebase` (`firestore`, `ai_logic`), SHA-1 verified in `app/google-services.json`, and `DeployRules` executed cleanly | **DEPLOYED** |

## 2. Implemented & Compiled — Pending Physical Android Device Verification

Because no physical phone or ADB instrumentation runner is available inside the container, these hardware/OEM-dependent behaviors are compiled cleanly and ready for on-device QA:
- **Credential Manager Google Sign-In UI:** Interactive Google account sheet (`GetSignInWithGoogleOption`) and silent auto sign-in (`GetGoogleIdOption`) on Google Play Services.
- **Floating Overlay Service (`FloatingCaptureService`):** `SYSTEM_ALERT_WINDOW` permission grant flow, edge snapping across screen rotations, IME keyboard resize behavior over third-party video players, and foreground notification behavior on Android 14+.
- **Push-to-Talk English Voice Input (`VoiceCaptureController`):** `RECORD_AUDIO` runtime permission prompt, `SpeechRecognizer` (`en-US`) recognition latency, and OEM restriction fallback from overlay into `VoiceQuickCaptureScreen`.
- **Android TTS & Media3 Playback (`ListeningPlayerController`):** Voice synthesis quality and rate calibration across Android OEMs, plus remote `.mp3`/`.m4a` streaming when lessons are upgraded to `audioMode = "MEDIA3"`.
- **AI Word Insight (`FirebaseAiWordHelperRepository`):** Live `gemini-3.8-flash` call protected by App Check token verification in debug/release device builds.
