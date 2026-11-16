package com.example.core.firebase

import android.content.Context
import com.example.R
import com.example.core.common.ExpressionNormalizer
import com.example.core.data.UserDataRepository
import com.example.core.firebase.FirestoreMappers.toAttempt
import com.example.core.firebase.FirestoreMappers.toContextExample
import com.example.core.firebase.FirestoreMappers.toReviewEvent
import com.example.core.firebase.FirestoreMappers.toReviewState
import com.example.core.firebase.FirestoreMappers.toStudySession
import com.example.core.firebase.FirestoreMappers.toUserProfile
import com.example.core.firebase.FirestoreMappers.toVocabularyEntry
import com.example.core.model.ContextExample
import com.example.core.model.ContextSourceType
import com.example.core.model.QuestionAttempt
import com.example.core.model.ReviewEvent
import com.example.core.model.ReviewRating
import com.example.core.model.ReviewState
import com.example.core.model.SpacedRepetitionEngine
import com.example.core.model.StudySession
import com.example.core.model.UserDataExportBundle
import com.example.core.model.UserProfile
import com.example.core.model.UserSettings
import com.example.core.model.VocabularyEntry
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirestoreClientProvider {
    @Volatile
    private var configured = false

    fun getInstance(context: Context): FirebaseFirestore {
        val appContext = context.applicationContext
        val dbId = appContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        if (!configured) {
            synchronized(this) {
                if (!configured) {
                    runCatching {
                        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(
                                PersistentCacheSettings.newBuilder().build()
                            )
                            .build()
                    }
                    configured = true
                }
            }
        }
        return db
    }
}

class FirestoreUserDataRepository(
    private val db: FirebaseFirestore,
    override val ownerId: String
) : UserDataRepository {

    constructor(context: Context, userId: String) : this(
        db = FirestoreClientProvider.getInstance(context),
        ownerId = userId
    )

    override val isDemoMode: Boolean = false

    private fun userDoc() = db.collection("users").document(ownerId)
    private fun vocabCol() = userDoc().collection("vocabulary")
    private fun contextsCol() = userDoc().collection("contexts")
    private fun reviewStatesCol() = userDoc().collection("reviewStates")
    private fun reviewEventsCol() = userDoc().collection("reviewEvents")
    private fun sessionsCol() = userDoc().collection("studySessions")
    private fun attemptsCol() = userDoc().collection("attempts")
    private fun settingsDoc() = userDoc().collection("settings").document("preferences")

    override fun observeUserProfile(): Flow<UserProfile?> =
        userDoc().snapshots(MetadataChanges.INCLUDE)
            .map { snapshot -> if (snapshot.exists()) snapshot.toUserProfile() else null }
            .catch { error ->
                unwrapAndLog(error, OperationType.GET, userDoc().path)
                throw error
            }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = runCatching {
        val docRef = userDoc()
        val existing = docRef.get().await()
        val payload = mutableMapOf<String, Any>(
            "userId" to ownerId,
            "targetExam" to profile.targetExam.code,
            "cefrLevel" to profile.cefrLevel.code,
            "dailyGoalMinutes" to profile.dailyGoalMinutes.coerceIn(5, 240).toLong(),
            "onboardingCompleted" to profile.onboardingCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        profile.displayName?.takeIf { it.isNotBlank() }?.let { payload["displayName"] = it.take(120) }
        profile.email?.takeIf { it.isNotBlank() }?.let { payload["email"] = it.take(200) }
        if (!existing.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.set(payload, SetOptions.merge()).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, userDoc().path)
    }

    override fun observeVocabulary(): Flow<List<VocabularyEntry>> =
        vocabCol().orderBy("updatedAt", Query.Direction.DESCENDING).limit(200)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap -> snap.documents.mapNotNull { it.toVocabularyEntry() } }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, vocabCol().path)
                throw error
            }

    override fun observeContexts(): Flow<List<ContextExample>> =
        contextsCol().orderBy("createdAt", Query.Direction.DESCENDING).limit(200)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap -> snap.documents.mapNotNull { it.toContextExample() } }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, contextsCol().path)
                throw error
            }

    override fun observeReviewStates(): Flow<List<ReviewState>> =
        reviewStatesCol().limit(200)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap -> snap.documents.mapNotNull { it.toReviewState() } }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, reviewStatesCol().path)
                throw error
            }

    override fun observeReviewEvents(): Flow<List<ReviewEvent>> =
        reviewEventsCol().orderBy("reviewedAtEpochMillis", Query.Direction.DESCENDING).limit(200)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toReviewEvent() } }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, reviewEventsCol().path)
                throw error
            }

    override fun observeStudySessions(): Flow<List<StudySession>> =
        sessionsCol().orderBy("completedAtEpochMillis", Query.Direction.DESCENDING).limit(200)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toStudySession() } }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, sessionsCol().path)
                throw error
            }

    override fun observeAttempts(): Flow<List<QuestionAttempt>> =
        attemptsCol().orderBy("answeredAtEpochMillis", Query.Direction.DESCENDING).limit(200)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toAttempt() } }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, attemptsCol().path)
                throw error
            }

    override suspend fun saveVocabularyWithOptionalContext(
        expression: String,
        meaningTr: String,
        definitionEn: String,
        partOfSpeech: String,
        personalNote: String,
        tags: List<String>,
        favorite: Boolean,
        contextSentence: String,
        sourceTitle: String,
        sourceType: ContextSourceType,
        optionalSourceUrl: String?,
        optionalTimestampSeconds: Int?
    ): Result<String> = runCatching {
        val cleaned = expression.trim()
        require(cleaned.isNotEmpty()) { "Expression cannot be blank" }
        val normalized = ExpressionNormalizer.normalize(cleaned)
        val now = System.currentTimeMillis()

        // Deduplicate against cached/online user vocabulary by normalizedExpression
        val existingSnap = vocabCol()
            .whereEqualTo("normalizedExpression", normalized)
            .limit(1)
            .get()
            .await()
        val existingDoc = existingSnap.documents.firstOrNull()

        val batch = db.batch()
        val vocabId: String

        if (existingDoc == null) {
            vocabId = "v_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val vocabRef = vocabCol().document(vocabId)
            val vocabMap = FirestoreMappers.buildVocabularyCreateMap(
                userId = ownerId,
                expression = cleaned,
                normalizedExpression = normalized,
                meaningTr = meaningTr.trim(),
                definitionEn = definitionEn.trim(),
                partOfSpeech = partOfSpeech.trim(),
                personalNote = personalNote.trim(),
                tags = tags,
                favorite = favorite
            )
            batch.set(vocabRef, vocabMap)

            val initialState = SpacedRepetitionEngine.initialState(vocabId, now)
            batch.set(
                reviewStatesCol().document(vocabId),
                FirestoreMappers.buildReviewStateCreateMap(ownerId, initialState)
            )
        } else {
            vocabId = existingDoc.id
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (meaningTr.isNotBlank()) updates["meaningTr"] = meaningTr.trim().take(500)
            if (definitionEn.isNotBlank()) updates["definitionEn"] = definitionEn.trim().take(500)
            if (partOfSpeech.isNotBlank()) updates["partOfSpeech"] = partOfSpeech.trim().take(40)
            if (personalNote.isNotBlank()) updates["personalNote"] = personalNote.trim().take(1000)
            batch.set(existingDoc.reference, updates, SetOptions.merge())
        }

        if (contextSentence.isNotBlank()) {
            val ctxId = "c_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val ctxMap = mutableMapOf<String, Any>(
                "userId" to ownerId,
                "vocabularyId" to vocabId,
                "sentence" to contextSentence.trim().take(1000),
                "sourceType" to sourceType.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (sourceTitle.isNotBlank()) ctxMap["sourceTitle"] = sourceTitle.trim().take(200)
            if (!optionalSourceUrl.isNullOrBlank()) ctxMap["optionalSourceUrl"] = optionalSourceUrl.trim().take(500)
            if (optionalTimestampSeconds != null && optionalTimestampSeconds >= 0) {
                ctxMap["optionalTimestampSeconds"] = optionalTimestampSeconds.toLong()
            }
            batch.set(contextsCol().document(ctxId), ctxMap)
        }

        batch.commit().await()
        vocabId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, vocabCol().path)
    }

    override suspend fun updateVocabulary(entry: VocabularyEntry): Result<Unit> = runCatching {
        require(entry.id.isNotBlank()) { "Vocabulary ID required for update" }
        val docRef = vocabCol().document(entry.id)
        val payload = mutableMapOf<String, Any>(
            "expression" to entry.expression.trim().take(200),
            "normalizedExpression" to ExpressionNormalizer.normalize(entry.expression).take(200),
            "tags" to entry.tags.take(10),
            "favorite" to entry.favorite,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (entry.meaningTr.isNotBlank()) payload["meaningTr"] = entry.meaningTr.trim().take(500)
        if (entry.definitionEn.isNotBlank()) payload["definitionEn"] = entry.definitionEn.trim().take(500)
        if (entry.partOfSpeech.isNotBlank()) payload["partOfSpeech"] = entry.partOfSpeech.trim().take(40)
        if (entry.personalNote.isNotBlank()) payload["personalNote"] = entry.personalNote.trim().take(1000)
        docRef.set(payload, SetOptions.merge()).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, vocabCol().document(entry.id).path)
    }

    override suspend fun toggleFavorite(vocabularyId: String, favorite: Boolean): Result<Unit> = runCatching {
        val docRef = vocabCol().document(vocabularyId)
        docRef.set(
            mapOf(
                "favorite" to favorite,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, vocabCol().document(vocabularyId).path)
    }

    override suspend fun addContextExample(
        vocabularyId: String,
        sentence: String,
        sourceTitle: String,
        sourceType: ContextSourceType,
        optionalSourceUrl: String?,
        optionalTimestampSeconds: Int?
    ): Result<String> = runCatching {
        val cleaned = sentence.trim()
        require(cleaned.isNotEmpty()) { "Context sentence cannot be empty" }
        val ctxId = "c_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val map = mutableMapOf<String, Any>(
            "userId" to ownerId,
            "vocabularyId" to vocabularyId,
            "sentence" to cleaned.take(1000),
            "sourceType" to sourceType.code,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (sourceTitle.isNotBlank()) map["sourceTitle"] = sourceTitle.trim().take(200)
        if (!optionalSourceUrl.isNullOrBlank()) map["optionalSourceUrl"] = optionalSourceUrl.trim().take(500)
        if (optionalTimestampSeconds != null && optionalTimestampSeconds >= 0) {
            map["optionalTimestampSeconds"] = optionalTimestampSeconds.toLong()
        }
        contextsCol().document(ctxId).set(map).await()
        ctxId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, contextsCol().path)
    }

    override suspend fun deleteVocabularyAndRelated(vocabularyId: String): Result<Unit> = runCatching {
        val relatedContexts = contextsCol().whereEqualTo("vocabularyId", vocabularyId).get().await()
        val batch = db.batch()
        batch.delete(vocabCol().document(vocabularyId))
        batch.delete(reviewStatesCol().document(vocabularyId))
        relatedContexts.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, vocabCol().document(vocabularyId).path)
    }

    override suspend fun submitFlashcardReview(
        vocabularyId: String,
        rating: ReviewRating,
        eventId: String,
        nowEpochMillis: Long
    ): Result<ReviewState> = runCatching {
        val eventRef = reviewEventsCol().document(eventId)
        val stateRef = reviewStatesCol().document(vocabularyId)

        // Idempotent check: avoid duplicate counting on retry
        val existingEvent = eventRef.get().await()
        if (existingEvent.exists()) {
            val stateSnap = stateRef.get().await()
            return@runCatching stateSnap.toReviewState()
                ?: SpacedRepetitionEngine.initialState(vocabularyId, nowEpochMillis)
        }

        val stateSnap = stateRef.get().await()
        val currentState = stateSnap.toReviewState()
            ?: SpacedRepetitionEngine.initialState(vocabularyId, nowEpochMillis)
        val outcome = SpacedRepetitionEngine.applyReview(currentState, rating, eventId, nowEpochMillis)

        val batch = db.batch()
        if (stateSnap.exists()) {
            val updateMap = mutableMapOf<String, Any>(
                "dueAtEpochMillis" to outcome.nextState.dueAtEpochMillis,
                "intervalDays" to outcome.nextState.intervalDays.toLong(),
                "easeFactor" to outcome.nextState.easeFactor,
                "repetitionCount" to outcome.nextState.repetitionCount.toLong(),
                "lapseCount" to outcome.nextState.lapseCount.toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            outcome.nextState.lastReviewedAtEpochMillis?.let { updateMap["lastReviewedAtEpochMillis"] = it }
            batch.set(stateRef, updateMap, SetOptions.merge())
        } else {
            batch.set(stateRef, FirestoreMappers.buildReviewStateCreateMap(ownerId, outcome.nextState))
        }

        val eventMap = mapOf(
            "userId" to ownerId,
            "eventId" to eventId,
            "vocabularyId" to vocabularyId,
            "rating" to rating.code,
            "previousIntervalDays" to outcome.reviewEvent.previousIntervalDays.toLong(),
            "newIntervalDays" to outcome.reviewEvent.newIntervalDays.toLong(),
            "reviewedAtEpochMillis" to outcome.reviewEvent.reviewedAtEpochMillis,
            "createdAt" to FieldValue.serverTimestamp()
        )
        batch.set(eventRef, eventMap)
        batch.commit().await()
        outcome.nextState
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, reviewEventsCol().document(eventId).path)
    }

    override suspend fun recordPracticeCompletion(
        session: StudySession,
        attempts: List<QuestionAttempt>
    ): Result<Unit> = runCatching {
        val batch = db.batch()
        val sessionId = session.id.ifBlank { "s_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val sessionMap = mapOf(
            "userId" to ownerId,
            "moduleType" to session.moduleType.code,
            "topicOrContentId" to session.topicOrContentId.take(128),
            "durationSeconds" to session.durationSeconds.coerceIn(0, 28800).toLong(),
            "totalItems" to session.totalItems.coerceIn(0, 500).toLong(),
            "correctCount" to session.correctCount.coerceIn(0, session.totalItems.coerceAtLeast(0)).toLong(),
            "dateKey" to session.dateKey.take(20),
            "startedAtEpochMillis" to session.startedAtEpochMillis,
            "completedAtEpochMillis" to session.completedAtEpochMillis.coerceAtLeast(session.startedAtEpochMillis),
            "createdAt" to FieldValue.serverTimestamp()
        )
        batch.set(sessionsCol().document(sessionId), sessionMap)

        attempts.forEach { a ->
            val attemptId = a.id.ifBlank { "a_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
            val aMap = mutableMapOf<String, Any>(
                "userId" to ownerId,
                "moduleType" to a.moduleType.code,
                "contentId" to a.contentId.take(128),
                "questionId" to a.questionId.take(128),
                "selectedOptionId" to a.selectedOptionId.take(64),
                "correctOptionId" to a.correctOptionId.take(64),
                "isCorrect" to a.isCorrect,
                "answeredAtEpochMillis" to a.answeredAtEpochMillis,
                "createdAt" to FieldValue.serverTimestamp()
            )
            a.questionType?.takeIf { it.isNotBlank() }?.let { aMap["questionType"] = it.take(64) }
            a.topicId?.takeIf { it.isNotBlank() }?.let { aMap["topicId"] = it.take(128) }
            batch.set(attemptsCol().document(attemptId), aMap)
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, sessionsCol().path)
    }

    override suspend fun syncSettingsToCloud(settings: UserSettings): Result<Unit> = runCatching {
        val docRef = settingsDoc()
        val existing = docRef.get().await()
        val map = mutableMapOf<String, Any>(
            "userId" to ownerId,
            "language" to settings.language.code,
            "themeMode" to settings.themeMode.code,
            "dailyGoalMinutes" to settings.dailyGoalMinutes.coerceIn(5, 240).toLong(),
            "targetExam" to settings.targetExam.code,
            "cefrLevel" to settings.cefrLevel.code,
            "overlayScale" to settings.overlayScale.toDouble().coerceIn(0.7, 1.5),
            "overlayAlpha" to settings.overlayAlpha.toDouble().coerceIn(0.4, 1.0),
            "readingFontSizeSp" to settings.readingFontSizeSp.coerceIn(14, 28).toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!existing.exists()) {
            map["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(map).await()
        } else {
            docRef.set(map, SetOptions.merge()).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, settingsDoc().path)
    }

    override suspend fun importBundle(bundle: UserDataExportBundle): Result<Int> = runCatching {
        var importedCount = 0
        val batch = db.batch()
        bundle.vocabulary.take(150).forEach { v ->
            val docRef = vocabCol().document(v.id)
            val map = FirestoreMappers.buildVocabularyCreateMap(
                userId = ownerId,
                expression = v.expression,
                normalizedExpression = ExpressionNormalizer.normalize(v.expression),
                meaningTr = v.meaningTr,
                definitionEn = v.definitionEn,
                partOfSpeech = v.partOfSpeech,
                personalNote = v.personalNote,
                tags = v.tags,
                favorite = v.favorite
            )
            batch.set(docRef, map)
            importedCount++
        }
        bundle.contexts.take(150).forEach { c ->
            val docRef = contextsCol().document(c.id)
            val map = mutableMapOf<String, Any>(
                "userId" to ownerId,
                "vocabularyId" to c.vocabularyId,
                "sentence" to c.sentence.take(1000),
                "sourceType" to ContextSourceType.fromCode(c.sourceType).code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (c.sourceTitle.isNotBlank()) map["sourceTitle"] = c.sourceTitle.take(200)
            c.optionalSourceUrl?.takeIf { it.isNotBlank() }?.let { map["optionalSourceUrl"] = it.take(500) }
            c.optionalTimestampSeconds?.takeIf { it >= 0 }?.let { map["optionalTimestampSeconds"] = it.toLong() }
            batch.set(docRef, map)
        }
        bundle.reviewStates.take(150).forEach { r ->
            val st = ReviewState(
                vocabularyId = r.vocabularyId,
                dueAtEpochMillis = r.dueAtEpochMillis,
                intervalDays = r.intervalDays,
                easeFactor = r.easeFactor,
                repetitionCount = r.repetitionCount,
                lapseCount = r.lapseCount,
                lastReviewedAtEpochMillis = r.lastReviewedAtEpochMillis
            )
            batch.set(reviewStatesCol().document(r.vocabularyId), FirestoreMappers.buildReviewStateCreateMap(ownerId, st))
        }
        batch.commit().await()
        importedCount
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, vocabCol().path)
    }

    override suspend fun deleteAllUserData(): Result<Unit> = runCatching {
        val subcollections = listOf(
            vocabCol(),
            contextsCol(),
            reviewStatesCol(),
            reviewEventsCol(),
            sessionsCol(),
            attemptsCol(),
            userDoc().collection("settings")
        )
        for (col in subcollections) {
            while (true) {
                val snap = col.limit(200).get().await()
                if (snap.isEmpty) break
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        userDoc().delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, userDoc().path)
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, op, path)
    }
}
