package com.example.core.data

import com.example.core.model.ContextExample
import com.example.core.model.ContextSourceType
import com.example.core.model.GrammarTopic
import com.example.core.model.ListeningLesson
import com.example.core.model.QuestionAttempt
import com.example.core.model.ReadingPassage
import com.example.core.model.ReviewEvent
import com.example.core.model.ReviewRating
import com.example.core.model.ReviewState
import com.example.core.model.StudySession
import com.example.core.model.UserDataExportBundle
import com.example.core.model.UserProfile
import com.example.core.model.UserSettings
import com.example.core.model.VocabularyEntry
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    val isDemoMode: Boolean
    val ownerId: String

    fun observeUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>

    fun observeVocabulary(): Flow<List<VocabularyEntry>>
    fun observeContexts(): Flow<List<ContextExample>>
    fun observeReviewStates(): Flow<List<ReviewState>>
    fun observeReviewEvents(): Flow<List<ReviewEvent>>
    fun observeStudySessions(): Flow<List<StudySession>>
    fun observeAttempts(): Flow<List<QuestionAttempt>>

    suspend fun saveVocabularyWithOptionalContext(
        expression: String,
        meaningTr: String = "",
        definitionEn: String = "",
        partOfSpeech: String = "",
        personalNote: String = "",
        tags: List<String> = emptyList(),
        favorite: Boolean = false,
        contextSentence: String = "",
        sourceTitle: String = "",
        sourceType: ContextSourceType = ContextSourceType.MANUAL,
        optionalSourceUrl: String? = null,
        optionalTimestampSeconds: Int? = null
    ): Result<String>

    suspend fun updateVocabulary(entry: VocabularyEntry): Result<Unit>
    suspend fun toggleFavorite(vocabularyId: String, favorite: Boolean): Result<Unit>

    suspend fun addContextExample(
        vocabularyId: String,
        sentence: String,
        sourceTitle: String = "",
        sourceType: ContextSourceType = ContextSourceType.MANUAL,
        optionalSourceUrl: String? = null,
        optionalTimestampSeconds: Int? = null
    ): Result<String>

    suspend fun deleteVocabularyAndRelated(vocabularyId: String): Result<Unit>

    suspend fun submitFlashcardReview(
        vocabularyId: String,
        rating: ReviewRating,
        eventId: String,
        nowEpochMillis: Long
    ): Result<ReviewState>

    suspend fun recordPracticeCompletion(
        session: StudySession,
        attempts: List<QuestionAttempt>
    ): Result<Unit>

    suspend fun syncSettingsToCloud(settings: UserSettings): Result<Unit>
    suspend fun importBundle(bundle: UserDataExportBundle): Result<Int>
    suspend fun deleteAllUserData(): Result<Unit>
}

interface ContentRepository {
    suspend fun getGrammarTopics(): List<GrammarTopic>
    suspend fun getReadingPassages(): List<ReadingPassage>
    suspend fun getListeningLessons(): List<ListeningLesson>
}

interface SettingsRepository {
    val settingsFlow: Flow<UserSettings>
    val lastStudyRouteFlow: Flow<String?>
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings)
    suspend fun saveLastStudyRoute(route: String)
    fun getOverlayPosition(): Pair<Int, Int>
    fun saveOverlayPosition(x: Int, y: Int)
}

data class AiWordInsight(
    val meaningTr: String,
    val definitionEn: String,
    val partOfSpeech: String,
    val exampleSentence: String,
    val usageNoteTr: String
)

interface AiWordHelperRepository {
    suspend fun generateWordInsight(
        expression: String,
        contextSentence: String?
    ): Result<AiWordInsight>
}
