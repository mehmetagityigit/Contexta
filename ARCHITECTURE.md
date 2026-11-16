# Architecture Overview

Contexta follows a clean single-module MVVM architecture (`app`) with explicit package boundaries so domain logic stays Android-independent and backend implementations remain swappable.

## Package Structure

- `core/model/`: Pure Kotlin domain models (`VocabularyEntry`, `ContextExample`, `ReviewState`, `StudySession`, `QuestionAttempt`), deterministic `SpacedRepetitionEngine` (SM-2), `PracticeScoringEngine`, `ContentValidator`, and JSON export/import DTOs. Uses `java.util.Calendar` + `TimeZone` for full API 24+ and JVM compatibility.
- `core/common/`: Platform utilities — `ExpressionNormalizer`, `ConnectivityObserver`, `LocaleHelper`, `VoiceCaptureController` (push-to-talk `SpeechRecognizer`), and `ListeningPlayerController` (dual Media3 ExoPlayer + Android TTS).
- `core/designsystem/`: Centralized Material 3 theme (`Color.kt`, `Shapes.kt`, `Type.kt`, `Theme.kt`) and reusable components (`ContextaCard`, `StatusPill`, `SyncBadge`, `DemoModeBanner`, empty/loading/error views).
- `core/navigation/`: Type-safe `@Serializable` Navigation Compose route keys (`AppDestinations.kt`).
- `core/data/`: Repository interfaces (`UserDataRepository`, `ContentRepository`, `SettingsRepository`, `AiWordHelperRepository`), `DemoUserDataRepository` (in-memory seeded demo mode), `AssetContentRepository`, `DataStoreSettingsRepository`, `UserDataTransferManager`, and `AppContainer`.
- `core/firebase/`: Firebase implementation — `AppCheckConfig` (Debug / reCAPTCHA Enterprise), `GoogleAuthManager` (`CredentialManager`), `FirestoreUserDataRepository` (targeting named Firestore database via `R.string.firestore_database_id`), `FirestoreMappers`, `FirestoreErrorHandler`, and `FirebaseAiWordHelperRepository` (on-demand `gemini-3.8-flash` structured JSON suggestions).
- `feature/*`: Feature-scoped ViewModels and Compose screens (`auth`, `home`, `vocabulary`, `overlay`, `study`, `grammar`, `reading`, `listening`, `progress`, `settings`).

## Data Flow & Rules

1. **Composable → ViewModel → Repository Interface → Data Source**: Composables never import or call Firebase SDK classes directly.
2. **SDK Isolation**: Firestore `DocumentSnapshot` and Firebase `FirebaseUser` do not leak into domain models; `FirestoreMappers` converts documents at the repository boundary.
3. **Demo Isolation**: `DemoUserDataRepository` lives solely in memory and tags items with `SyncStatus.DEMO_LOCAL`. Signed-in Firebase accounts use `FirestoreUserDataRepository` with zero automatic demo seeding.
4. **Adaptive Layout**: `MainActivity` switches between `NavigationBar` (`< 600.dp`) and `NavigationRail` (`>= 600.dp`), with `widthIn(max = 600.dp)` content constraints on tablets.
