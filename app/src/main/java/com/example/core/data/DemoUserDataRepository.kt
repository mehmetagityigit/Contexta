package com.example.core.data

import com.example.core.common.ExpressionNormalizer
import com.example.core.model.ContextExample
import com.example.core.model.ContextSourceType
import com.example.core.model.QuestionAttempt
import com.example.core.model.ReviewEvent
import com.example.core.model.ReviewRating
import com.example.core.model.ReviewState
import com.example.core.model.SpacedRepetitionEngine
import com.example.core.model.StudySession
import com.example.core.model.SyncStatus
import com.example.core.model.UserDataExportBundle
import com.example.core.model.UserProfile
import com.example.core.model.UserSettings
import com.example.core.model.VocabularyEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Isolated in-memory Demo repository seeded with 20 authentic vocabulary items and contexts.
 * Never touches Firestore or production user collections.
 */
class DemoUserDataRepository : UserDataRepository {
    override val isDemoMode: Boolean = true
    override val ownerId: String = "demo_local_user"

    private val profileFlow = MutableStateFlow<UserProfile?>(
        UserProfile(
            userId = ownerId,
            displayName = "Demo",
            onboardingCompleted = false
        )
    )
    private val vocabularyFlow = MutableStateFlow<List<VocabularyEntry>>(emptyList())
    private val contextsFlow = MutableStateFlow<List<ContextExample>>(emptyList())
    private val reviewStatesFlow = MutableStateFlow<List<ReviewState>>(emptyList())
    private val reviewEventsFlow = MutableStateFlow<List<ReviewEvent>>(emptyList())
    private val sessionsFlow = MutableStateFlow<List<StudySession>>(emptyList())
    private val attemptsFlow = MutableStateFlow<List<QuestionAttempt>>(emptyList())

    init {
        seedDemoVocabulary()
    }

    override fun observeUserProfile(): Flow<UserProfile?> = profileFlow.asStateFlow()
    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        profileFlow.value = profile.copy(userId = ownerId)
        return Result.success(Unit)
    }

    override fun observeVocabulary(): Flow<List<VocabularyEntry>> = vocabularyFlow.asStateFlow()
    override fun observeContexts(): Flow<List<ContextExample>> = contextsFlow.asStateFlow()
    override fun observeReviewStates(): Flow<List<ReviewState>> = reviewStatesFlow.asStateFlow()
    override fun observeReviewEvents(): Flow<List<ReviewEvent>> = reviewEventsFlow.asStateFlow()
    override fun observeStudySessions(): Flow<List<StudySession>> = sessionsFlow.asStateFlow()
    override fun observeAttempts(): Flow<List<QuestionAttempt>> = attemptsFlow.asStateFlow()

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
    ): Result<String> {
        val cleaned = expression.trim()
        if (cleaned.isEmpty()) return Result.failure(IllegalArgumentException("Expression cannot be empty"))
        val normalized = ExpressionNormalizer.normalize(cleaned)
        val now = System.currentTimeMillis()
        val existing = vocabularyFlow.value.firstOrNull { it.normalizedExpression == normalized }
        val vocabId = existing?.id ?: "demo_v_${UUID.randomUUID().toString().take(8)}"

        if (existing == null) {
            val entry = VocabularyEntry(
                id = vocabId,
                expression = cleaned,
                normalizedExpression = normalized,
                meaningTr = meaningTr.trim(),
                definitionEn = definitionEn.trim(),
                partOfSpeech = partOfSpeech.trim(),
                personalNote = personalNote.trim(),
                tags = tags.take(10),
                favorite = favorite,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                syncStatus = SyncStatus.DEMO_LOCAL
            )
            vocabularyFlow.update { listOf(entry) + it }
            reviewStatesFlow.update { it + SpacedRepetitionEngine.initialState(vocabId, now).copy(syncStatus = SyncStatus.DEMO_LOCAL) }
        } else {
            vocabularyFlow.update { list ->
                list.map {
                    if (it.id == vocabId) {
                        it.copy(
                            meaningTr = meaningTr.ifBlank { it.meaningTr },
                            definitionEn = definitionEn.ifBlank { it.definitionEn },
                            partOfSpeech = partOfSpeech.ifBlank { it.partOfSpeech },
                            personalNote = personalNote.ifBlank { it.personalNote },
                            tags = (it.tags + tags).distinct().take(10),
                            updatedAtEpochMillis = now
                        )
                    } else it
                }
            }
        }

        if (contextSentence.isNotBlank()) {
            val ctx = ContextExample(
                id = "demo_c_${UUID.randomUUID().toString().take(8)}",
                vocabularyId = vocabId,
                sentence = contextSentence.trim(),
                sourceTitle = sourceTitle.trim(),
                sourceType = sourceType,
                optionalSourceUrl = optionalSourceUrl,
                optionalTimestampSeconds = optionalTimestampSeconds,
                createdAtEpochMillis = now,
                syncStatus = SyncStatus.DEMO_LOCAL
            )
            contextsFlow.update { listOf(ctx) + it }
        }
        return Result.success(vocabId)
    }

    override suspend fun updateVocabulary(entry: VocabularyEntry): Result<Unit> {
        val now = System.currentTimeMillis()
        vocabularyFlow.update { list ->
            list.map {
                if (it.id == entry.id) {
                    entry.copy(
                        normalizedExpression = ExpressionNormalizer.normalize(entry.expression),
                        updatedAtEpochMillis = now,
                        syncStatus = SyncStatus.DEMO_LOCAL
                    )
                } else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun toggleFavorite(vocabularyId: String, favorite: Boolean): Result<Unit> {
        val now = System.currentTimeMillis()
        vocabularyFlow.update { list ->
            list.map {
                if (it.id == vocabularyId) it.copy(favorite = favorite, updatedAtEpochMillis = now) else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun addContextExample(
        vocabularyId: String,
        sentence: String,
        sourceTitle: String,
        sourceType: ContextSourceType,
        optionalSourceUrl: String?,
        optionalTimestampSeconds: Int?
    ): Result<String> {
        val cleaned = sentence.trim()
        if (cleaned.isEmpty()) return Result.failure(IllegalArgumentException("Sentence cannot be empty"))
        val ctx = ContextExample(
            id = "demo_c_${UUID.randomUUID().toString().take(8)}",
            vocabularyId = vocabularyId,
            sentence = cleaned,
            sourceTitle = sourceTitle.trim(),
            sourceType = sourceType,
            optionalSourceUrl = optionalSourceUrl,
            optionalTimestampSeconds = optionalTimestampSeconds,
            createdAtEpochMillis = System.currentTimeMillis(),
            syncStatus = SyncStatus.DEMO_LOCAL
        )
        contextsFlow.update { listOf(ctx) + it }
        return Result.success(ctx.id)
    }

    override suspend fun deleteVocabularyAndRelated(vocabularyId: String): Result<Unit> {
        vocabularyFlow.update { it.filterNot { item -> item.id == vocabularyId } }
        contextsFlow.update { it.filterNot { ctx -> ctx.vocabularyId == vocabularyId } }
        reviewStatesFlow.update { it.filterNot { st -> st.vocabularyId == vocabularyId } }
        return Result.success(Unit)
    }

    override suspend fun submitFlashcardReview(
        vocabularyId: String,
        rating: ReviewRating,
        eventId: String,
        nowEpochMillis: Long
    ): Result<ReviewState> {
        if (reviewEventsFlow.value.any { it.eventId == eventId }) {
            val existing = reviewStatesFlow.value.firstOrNull { it.vocabularyId == vocabularyId }
                ?: SpacedRepetitionEngine.initialState(vocabularyId, nowEpochMillis)
            return Result.success(existing)
        }
        val current = reviewStatesFlow.value.firstOrNull { it.vocabularyId == vocabularyId }
            ?: SpacedRepetitionEngine.initialState(vocabularyId, nowEpochMillis)
        val outcome = SpacedRepetitionEngine.applyReview(current, rating, eventId, nowEpochMillis)
        val next = outcome.nextState.copy(syncStatus = SyncStatus.DEMO_LOCAL)
        reviewStatesFlow.update { list ->
            list.filterNot { it.vocabularyId == vocabularyId } + next
        }
        reviewEventsFlow.update { listOf(outcome.reviewEvent) + it }
        return Result.success(next)
    }

    override suspend fun recordPracticeCompletion(
        session: StudySession,
        attempts: List<QuestionAttempt>
    ): Result<Unit> {
        sessionsFlow.update { listOf(session) + it }
        attemptsFlow.update { attempts + it }
        return Result.success(Unit)
    }

    override suspend fun syncSettingsToCloud(settings: UserSettings): Result<Unit> = Result.success(Unit)

    override suspend fun importBundle(bundle: UserDataExportBundle): Result<Int> {
        var count = 0
        bundle.vocabulary.forEach { dto ->
            val entry = VocabularyEntry(
                id = dto.id,
                expression = dto.expression,
                normalizedExpression = ExpressionNormalizer.normalize(dto.expression),
                meaningTr = dto.meaningTr,
                definitionEn = dto.definitionEn,
                partOfSpeech = dto.partOfSpeech,
                personalNote = dto.personalNote,
                tags = dto.tags.take(10),
                favorite = dto.favorite,
                createdAtEpochMillis = dto.createdAtEpochMillis,
                updatedAtEpochMillis = dto.updatedAtEpochMillis,
                syncStatus = SyncStatus.DEMO_LOCAL
            )
            vocabularyFlow.update { list -> list.filterNot { it.id == entry.id } + entry }
            count++
        }
        bundle.contexts.forEach { c ->
            val ctx = ContextExample(
                id = c.id,
                vocabularyId = c.vocabularyId,
                sentence = c.sentence,
                sourceTitle = c.sourceTitle,
                sourceType = ContextSourceType.fromCode(c.sourceType),
                optionalSourceUrl = c.optionalSourceUrl,
                optionalTimestampSeconds = c.optionalTimestampSeconds,
                createdAtEpochMillis = c.createdAtEpochMillis,
                syncStatus = SyncStatus.DEMO_LOCAL
            )
            contextsFlow.update { list -> list.filterNot { it.id == ctx.id } + ctx }
        }
        bundle.reviewStates.forEach { r ->
            val st = ReviewState(
                vocabularyId = r.vocabularyId,
                dueAtEpochMillis = r.dueAtEpochMillis,
                intervalDays = r.intervalDays,
                easeFactor = r.easeFactor,
                repetitionCount = r.repetitionCount,
                lapseCount = r.lapseCount,
                lastReviewedAtEpochMillis = r.lastReviewedAtEpochMillis,
                syncStatus = SyncStatus.DEMO_LOCAL
            )
            reviewStatesFlow.update { list -> list.filterNot { it.vocabularyId == st.vocabularyId } + st }
        }
        return Result.success(count)
    }

    override suspend fun deleteAllUserData(): Result<Unit> {
        vocabularyFlow.value = emptyList()
        contextsFlow.value = emptyList()
        reviewStatesFlow.value = emptyList()
        reviewEventsFlow.value = emptyList()
        sessionsFlow.value = emptyList()
        attemptsFlow.value = emptyList()
        return Result.success(Unit)
    }

    private fun seedDemoVocabulary() {
        val baseTime = System.currentTimeMillis() - 86_400_000L
        data class Seed(
            val expr: String,
            val tr: String,
            val en: String,
            val pos: String,
            val tags: List<String>,
            val fav: Boolean,
            val sentence: String,
            val source: String,
            val type: ContextSourceType
        )

        val seeds = listOf(
            Seed("ubiquitous", "her yerde bulunan, yaygın", "present or found everywhere", "adj.", listOf("SAT", "Academic"), true, "Smartphones have become ubiquitous in modern classrooms.", "EdTech Essay", ContextSourceType.ARTICLE),
            Seed("mitigate", "hafifletmek, etkisini azaltmak", "to make something less severe or painful", "verb", listOf("IELTS", "SAT"), true, "Coastal wetlands help mitigate storm surges during hurricanes.", "Climate Lecture", ContextSourceType.VIDEO),
            Seed("ephemeral", "kısa ömürlü, geçici", "lasting for a very short time", "adj.", listOf("AP", "Literature"), false, "Morning frost on desert grass is an ephemeral phenomenon.", "Nature Essay", ContextSourceType.BOOK),
            Seed("corroborate", "doğrulamak, desteklemek", "to confirm or give support to a statement or finding", "verb", listOf("SAT", "Research"), false, "Ice-core samples corroborate the written weather diaries from 1816.", "History Seminar", ContextSourceType.PODCAST),
            Seed("caveat", "önemli uyarı, şerh", "a warning or condition attached to a statement", "noun", listOf("Academic", "IELTS"), false, "The survey shows rising confidence, with one important caveat regarding sample size.", "Statistics Note", ContextSourceType.ARTICLE),
            Seed("take into account", "hesaba katmak, göz önünde bulundurmak", "to consider a factor when making a decision", "phrase", listOf("IELTS", "General"), true, "Planners must take into account seasonal winds before siting turbines.", "Engineering Talk", ContextSourceType.VIDEO),
            Seed("by and large", "genel olarak, büyük ölçüde", "on the whole; considering everything", "phrase", listOf("General", "IELTS"), false, "By and large, the new transit schedule reduced morning delays.", "City Report", ContextSourceType.ARTICLE),
            Seed("draw a distinction", "ayrım yapmak", "to recognize a clear difference between two things", "phrase", listOf("AP", "Academic"), false, "The author draws a sharp distinction between correlation and causation.", "AP Seminar", ContextSourceType.BOOK),
            Seed("ambiguous", "belirsiz, iki anlamlı", "open to more than one interpretation", "adj.", listOf("SAT"), false, "The clause in the treaty remained deliberately ambiguous.", "Law Lecture", ContextSourceType.PODCAST),
            Seed("pragmatic", "gerçekçi, uygulamaya dönük", "dealing with things sensibly and practically", "adj.", listOf("IELTS", "SAT"), true, "Instead of waiting for ideal weather, they adopted a pragmatic schedule.", "Design Podcast", ContextSourceType.PODCAST),
            Seed("alleviate", "azaltmak, dindirmek", "to make suffering or a problem less severe", "verb", listOf("IELTS"), false, "Shaded bus shelters alleviate heat stress for commuters.", "Urban Studies", ContextSourceType.READING),
            Seed("disseminate", "yaymak, dağıtmak", "to spread information or ideas widely", "verb", listOf("AP", "Academic"), false, "Public libraries helped disseminate technical manuals across towns.", "Social History", ContextSourceType.READING),
            Seed("in light of", "... ışığında, ... göz önüne alındığında", "because of a particular fact or situation", "phrase", listOf("SAT", "IELTS"), false, "In light of the revised figures, the committee postponed the vote.", "Campus News", ContextSourceType.ARTICLE),
            Seed("hinge on", "...-e bağlı olmak, dayanmak", "to depend entirely on something", "phrasal verb", listOf("SAT", "Academic"), true, "The success of the restoration will hinge on controlling moisture.", "Art Conservation", ContextSourceType.BOOK),
            Seed("scarcity", "kıtlık, az bulunurluk", "the state of being scarce or in short supply", "noun", listOf("Economics", "IELTS"), false, "Freshwater scarcity forced towns to redesign irrigation canals.", "Econ Lecture", ContextSourceType.VIDEO),
            Seed("plausible", "makul, inandırıcı", "seeming reasonable or probable", "adj.", listOf("SAT", "AP"), false, "Astronomers offered a plausible explanation for the dim signal.", "Science Daily", ContextSourceType.ARTICLE),
            Seed("overhaul", "köklü biçimde yenilemek", "to examine and repair or modernize thoroughly", "verb", listOf("IELTS", "General"), false, "The transit authority plans to overhaul its ticketing app.", "Tech Brief", ContextSourceType.ARTICLE),
            Seed("resilient", "dayanıklı, çabuk toparlanan", "able to withstand or recover quickly from difficulty", "adj.", listOf("IELTS", "Academic"), false, "Native marsh grasses proved remarkably resilient after the flood.", "Ecology Clip", ContextSourceType.VIDEO),
            Seed("shed light on", "aydınlatmak, açıklığa kavuşturmak", "to help explain or make something clearer", "phrase", listOf("AP", "SAT"), false, "Newly digitized notebooks shed light on her early experiments.", "Archive Tour", ContextSourceType.PODCAST),
            Seed("tenuous", "zayıf, dayanaksız", "very weak or slight", "adj.", listOf("SAT", "C1"), false, "The link between the two manuscripts remains tenuous without dated ink tests.", "Philology Paper", ContextSourceType.BOOK)
        )

        val entries = mutableListOf<VocabularyEntry>()
        val contexts = mutableListOf<ContextExample>()
        val states = mutableListOf<ReviewState>()

        seeds.forEachIndexed { idx, s ->
            val id = "demo_seed_${idx + 1}"
            val ts = baseTime + idx * 60_000L
            entries += VocabularyEntry(
                id = id,
                expression = s.expr,
                normalizedExpression = ExpressionNormalizer.normalize(s.expr),
                meaningTr = s.tr,
                definitionEn = s.en,
                partOfSpeech = s.pos,
                tags = s.tags,
                favorite = s.fav,
                createdAtEpochMillis = ts,
                updatedAtEpochMillis = ts,
                syncStatus = SyncStatus.DEMO_LOCAL
            )
            contexts += ContextExample(
                id = "demo_ctx_${idx + 1}",
                vocabularyId = id,
                sentence = s.sentence,
                sourceTitle = s.source,
                sourceType = s.type,
                createdAtEpochMillis = ts,
                syncStatus = SyncStatus.DEMO_LOCAL
            )
            // First 8 cards are due immediately for review; remaining are scheduled ahead
            val dueOffset = if (idx < 8) -3_600_000L else (idx - 6) * 86_400_000L
            states += ReviewState(
                vocabularyId = id,
                dueAtEpochMillis = System.currentTimeMillis() + dueOffset,
                intervalDays = if (idx < 8) 0 else idx - 6,
                easeFactor = 2.5,
                repetitionCount = if (idx < 8) 0 else 1,
                lapseCount = 0,
                lastReviewedAtEpochMillis = if (idx < 8) null else ts,
                syncStatus = SyncStatus.DEMO_LOCAL
            )
        }

        vocabularyFlow.value = entries.reversed()
        contextsFlow.value = contexts.reversed()
        reviewStatesFlow.value = states
    }
}
