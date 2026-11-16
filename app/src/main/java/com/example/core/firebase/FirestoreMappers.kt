package com.example.core.firebase

import com.example.core.model.CefrLevel
import com.example.core.model.ContextExample
import com.example.core.model.ContextSourceType
import com.example.core.model.QuestionAttempt
import com.example.core.model.ReviewEvent
import com.example.core.model.ReviewRating
import com.example.core.model.ReviewState
import com.example.core.model.StudyModuleType
import com.example.core.model.StudySession
import com.example.core.model.SyncStatus
import com.example.core.model.TargetExam
import com.example.core.model.UserProfile
import com.example.core.model.VocabularyEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfileDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val targetExam: String = "GENERAL",
    val cefrLevel: String = "B2",
    val dailyGoalMinutes: Long = 20L,
    val onboardingCompleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VocabularyDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val expression: String = "",
    val normalizedExpression: String = "",
    val meaningTr: String? = null,
    val definitionEn: String? = null,
    val partOfSpeech: String? = null,
    val personalNote: String? = null,
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ContextExampleDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vocabularyId: String = "",
    val sentence: String = "",
    val sourceTitle: String? = null,
    val sourceType: String = "MANUAL",
    val optionalSourceUrl: String? = null,
    val optionalTimestampSeconds: Long? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReviewStateDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vocabularyId: String = "",
    val dueAtEpochMillis: Long = 0L,
    val intervalDays: Long = 0L,
    val easeFactor: Double = 2.5,
    val repetitionCount: Long = 0L,
    val lapseCount: Long = 0L,
    val lastReviewedAtEpochMillis: Long? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReviewEventDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventId: String = "",
    val vocabularyId: String = "",
    val rating: String = "GOOD",
    val previousIntervalDays: Long = 0L,
    val newIntervalDays: Long = 1L,
    val reviewedAtEpochMillis: Long = 0L,
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudySessionDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val moduleType: String = "FLASHCARD",
    val topicOrContentId: String = "",
    val durationSeconds: Long = 0L,
    val totalItems: Long = 0L,
    val correctCount: Long = 0L,
    val dateKey: String = "",
    val startedAtEpochMillis: Long = 0L,
    val completedAtEpochMillis: Long = 0L,
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttemptDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val moduleType: String = "GRAMMAR",
    val contentId: String = "",
    val questionId: String = "",
    val questionType: String? = null,
    val topicId: String? = null,
    val selectedOptionId: String = "",
    val correctOptionId: String = "",
    val isCorrect: Boolean = false,
    val answeredAtEpochMillis: Long = 0L,
    val createdAt: Timestamp? = null
)

object FirestoreMappers {
    private val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

    private fun syncStatusFor(snapshot: DocumentSnapshot): SyncStatus {
        return if (snapshot.metadata.hasPendingWrites()) SyncStatus.PENDING_LOCAL else SyncStatus.SYNCED
    }

    fun DocumentSnapshot.toUserProfile(): UserProfile? {
        val dto = toObject(UserProfileDto::class.java, est) ?: return null
        return UserProfile(
            userId = dto.userId.ifBlank { id },
            displayName = dto.displayName,
            email = dto.email,
            targetExam = TargetExam.fromCode(dto.targetExam),
            cefrLevel = CefrLevel.fromCode(dto.cefrLevel),
            dailyGoalMinutes = dto.dailyGoalMinutes.toInt().coerceIn(5, 240),
            onboardingCompleted = dto.onboardingCompleted
        )
    }

    fun DocumentSnapshot.toVocabularyEntry(): VocabularyEntry? {
        val dto = toObject(VocabularyDto::class.java, est) ?: return null
        if (dto.expression.isBlank()) return null
        return VocabularyEntry(
            id = id,
            expression = dto.expression,
            normalizedExpression = dto.normalizedExpression,
            meaningTr = dto.meaningTr.orEmpty(),
            definitionEn = dto.definitionEn.orEmpty(),
            partOfSpeech = dto.partOfSpeech.orEmpty(),
            personalNote = dto.personalNote.orEmpty(),
            tags = dto.tags,
            favorite = dto.favorite,
            createdAtEpochMillis = dto.createdAt?.toDate()?.time ?: 0L,
            updatedAtEpochMillis = dto.updatedAt?.toDate()?.time ?: 0L,
            syncStatus = syncStatusFor(this)
        )
    }

    fun DocumentSnapshot.toContextExample(): ContextExample? {
        val dto = toObject(ContextExampleDto::class.java, est) ?: return null
        if (dto.sentence.isBlank()) return null
        return ContextExample(
            id = id,
            vocabularyId = dto.vocabularyId,
            sentence = dto.sentence,
            sourceTitle = dto.sourceTitle.orEmpty(),
            sourceType = ContextSourceType.fromCode(dto.sourceType),
            optionalSourceUrl = dto.optionalSourceUrl,
            optionalTimestampSeconds = dto.optionalTimestampSeconds?.toInt(),
            createdAtEpochMillis = dto.createdAt?.toDate()?.time ?: 0L,
            syncStatus = syncStatusFor(this)
        )
    }

    fun DocumentSnapshot.toReviewState(): ReviewState? {
        val dto = toObject(ReviewStateDto::class.java, est) ?: return null
        return ReviewState(
            vocabularyId = dto.vocabularyId.ifBlank { id },
            dueAtEpochMillis = dto.dueAtEpochMillis,
            intervalDays = dto.intervalDays.toInt(),
            easeFactor = dto.easeFactor,
            repetitionCount = dto.repetitionCount.toInt(),
            lapseCount = dto.lapseCount.toInt(),
            lastReviewedAtEpochMillis = dto.lastReviewedAtEpochMillis,
            syncStatus = syncStatusFor(this)
        )
    }

    fun DocumentSnapshot.toReviewEvent(): ReviewEvent? {
        val dto = toObject(ReviewEventDto::class.java, est) ?: return null
        return ReviewEvent(
            eventId = dto.eventId.ifBlank { id },
            vocabularyId = dto.vocabularyId,
            rating = ReviewRating.fromCode(dto.rating),
            previousIntervalDays = dto.previousIntervalDays.toInt(),
            newIntervalDays = dto.newIntervalDays.toInt(),
            reviewedAtEpochMillis = dto.reviewedAtEpochMillis
        )
    }

    fun DocumentSnapshot.toStudySession(): StudySession? {
        val dto = toObject(StudySessionDto::class.java, est) ?: return null
        return StudySession(
            id = id,
            moduleType = StudyModuleType.fromCode(dto.moduleType),
            topicOrContentId = dto.topicOrContentId,
            durationSeconds = dto.durationSeconds.toInt(),
            totalItems = dto.totalItems.toInt(),
            correctCount = dto.correctCount.toInt(),
            dateKey = dto.dateKey,
            startedAtEpochMillis = dto.startedAtEpochMillis,
            completedAtEpochMillis = dto.completedAtEpochMillis
        )
    }

    fun DocumentSnapshot.toAttempt(): QuestionAttempt? {
        val dto = toObject(AttemptDto::class.java, est) ?: return null
        return QuestionAttempt(
            id = id,
            moduleType = StudyModuleType.fromCode(dto.moduleType),
            contentId = dto.contentId,
            questionId = dto.questionId,
            questionType = dto.questionType,
            topicId = dto.topicId,
            selectedOptionId = dto.selectedOptionId,
            correctOptionId = dto.correctOptionId,
            isCorrect = dto.isCorrect,
            answeredAtEpochMillis = dto.answeredAtEpochMillis
        )
    }

    fun buildVocabularyCreateMap(
        userId: String,
        expression: String,
        normalizedExpression: String,
        meaningTr: String,
        definitionEn: String,
        partOfSpeech: String,
        personalNote: String,
        tags: List<String>,
        favorite: Boolean
    ): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "expression" to expression.take(200),
            "normalizedExpression" to normalizedExpression.take(200),
            "tags" to tags.take(10),
            "favorite" to favorite,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (meaningTr.isNotBlank()) map["meaningTr"] = meaningTr.take(500)
        if (definitionEn.isNotBlank()) map["definitionEn"] = definitionEn.take(500)
        if (partOfSpeech.isNotBlank()) map["partOfSpeech"] = partOfSpeech.take(40)
        if (personalNote.isNotBlank()) map["personalNote"] = personalNote.take(1000)
        return map
    }

    fun buildReviewStateCreateMap(userId: String, state: ReviewState): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "vocabularyId" to state.vocabularyId,
            "dueAtEpochMillis" to state.dueAtEpochMillis,
            "intervalDays" to state.intervalDays.toLong(),
            "easeFactor" to state.easeFactor,
            "repetitionCount" to state.repetitionCount.toLong(),
            "lapseCount" to state.lapseCount.toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        state.lastReviewedAtEpochMillis?.let { map["lastReviewedAtEpochMillis"] = it }
        return map
    }
}
