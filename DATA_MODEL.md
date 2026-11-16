# Data Model & Firestore Security Schema

## User Collections (Owner-Scoped: `/users/{userId}/...`)

Every user document and subcollection enforces `request.auth != null && request.auth.uid == userId` in `firestore.rules`. Cross-user reads/writes and unauthenticated operations are denied.

1. **`/users/{userId}`**
   - `userId`, `displayName?`, `email?`, `targetExam` (`GENERAL` | `SAT` | `IELTS` | `AP_ACADEMIC`), `cefrLevel` (`B1` | `B2` | `C1` | `C2`), `dailyGoalMinutes` (`5..240`), `onboardingCompleted`, `createdAt`, `updatedAt`.
2. **`/users/{userId}/vocabulary/{vocabularyId}`**
   - `userId`, `expression` (`1..200`), `normalizedExpression` (lowercase trimmed for deduplication), `meaningTr`, `definitionEn`, `partOfSpeech`, `personalNote`, `tags` (`list`, max 10), `favorite` (`bool`), `createdAt`, `updatedAt`.
   - Separate from contexts so one expression can collect multiple real-world context sentences over time.
3. **`/users/{userId}/contexts/{contextId}`**
   - `userId`, `vocabularyId`, `sentence` (`1..1000`), `sourceTitle`, `sourceType` (`VIDEO` | `ARTICLE` | `BOOK` | `PODCAST` | `READING` | `LISTENING` | `OVERLAY` | `VOICE` | `MANUAL`), `optionalSourceUrl?`, `optionalTimestampSeconds?`, `createdAt`, `updatedAt`.
4. **`/users/{userId}/reviewStates/{vocabularyId}`**
   - `userId`, `vocabularyId`, `dueAtEpochMillis`, `intervalDays`, `easeFactor` (`1.3..5.0`), `repetitionCount`, `lapseCount`, `lastReviewedAtEpochMillis?`, `createdAt`, `updatedAt`.
5. **`/users/{userId}/reviewEvents/{eventId}`** (Append-only)
   - `userId`, `eventId` (used for idempotency check in `submitFlashcardReview`), `vocabularyId`, `rating` (`AGAIN` | `HARD` | `GOOD` | `EASY`), `previousIntervalDays`, `newIntervalDays`, `reviewedAtEpochMillis`, `createdAt`.
6. **`/users/{userId}/studySessions/{sessionId}`** (Append-only)
   - `userId`, `moduleType` (`FLASHCARD` | `GRAMMAR` | `READING` | `LISTENING`), `topicOrContentId`, `durationSeconds`, `totalItems`, `correctCount` (`<= totalItems`), `dateKey`, `startedAtEpochMillis`, `completedAtEpochMillis`, `createdAt`.
7. **`/users/{userId}/attempts/{attemptId}`** (Append-only)
   - `userId`, `moduleType` (`GRAMMAR` | `READING` | `LISTENING`), `contentId`, `questionId`, `questionType?`, `topicId?`, `selectedOptionId`, `correctOptionId`, `isCorrect`, `answeredAtEpochMillis`, `createdAt`.
8. **`/users/{userId}/settings/preferences`**
   - `userId`, `language` (`tr` | `en`), `themeMode` (`SYSTEM` | `LIGHT` | `DARK`), `dailyGoalMinutes`, `targetExam`, `cefrLevel`, `overlayScale`, `overlayAlpha`, `readingFontSizeSp`, `createdAt`, `updatedAt`.

## Public Read-Only Curriculum Collections

- `/contentGrammar/{contentId}`
- `/contentReading/{contentId}`
- `/contentListening/{contentId}`

Signed-in users can read (`get`/`list`) documents where `status == 'published'`. Mobile client writes (`create`/`update`/`delete`) are denied by rule.
