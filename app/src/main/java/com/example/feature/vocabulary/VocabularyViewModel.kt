package com.example.feature.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.common.ConnectivityObserver
import com.example.core.data.AiWordHelperRepository
import com.example.core.data.AiWordInsight
import com.example.core.data.UserDataRepository
import com.example.core.model.ContextSourceType
import com.example.core.model.PracticeScoringEngine
import com.example.core.model.ReviewRating
import com.example.core.model.SpacedRepetitionEngine
import com.example.core.model.StudyModuleType
import com.example.core.model.StudySession
import com.example.core.model.VocabularyEntry
import com.example.core.model.VocabularyFilter
import com.example.core.model.VocabularyWithContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class VocabularyListUiState(
    val isLoading: Boolean = true,
    val isDemoMode: Boolean = false,
    val isOnline: Boolean = true,
    val searchQuery: String = "",
    val selectedFilter: VocabularyFilter = VocabularyFilter.ALL,
    val items: List<VocabularyWithContext> = emptyList(),
    val dueCount: Int = 0,
    val totalCount: Int = 0
)

data class FlashcardSessionUiState(
    val isActive: Boolean = false,
    val queue: List<VocabularyWithContext> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val reviewedCount: Int = 0,
    val recalledCount: Int = 0,
    val againCount: Int = 0,
    val isCompleted: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

data class AiInsightUiState(
    val isLoading: Boolean = false,
    val insight: AiWordInsight? = null,
    val errorMessage: String? = null
)

class VocabularyViewModel(
    private val userDataRepository: UserDataRepository,
    connectivityObserver: ConnectivityObserver,
    private val aiWordHelperRepository: AiWordHelperRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(VocabularyFilter.ALL)

    private val allCombined: StateFlow<List<VocabularyWithContext>> = combine(
        userDataRepository.observeVocabulary(),
        userDataRepository.observeContexts(),
        userDataRepository.observeReviewStates()
    ) { vocab, contexts, states ->
        val ctxMap = contexts.groupBy { it.vocabularyId }
        val stateMap = states.associateBy { it.vocabularyId }
        vocab.sortedByDescending { it.updatedAtEpochMillis }.map { entry ->
            VocabularyWithContext(
                entry = entry,
                contexts = ctxMap[entry.id].orEmpty().sortedByDescending { it.createdAtEpochMillis },
                reviewState = stateMap[entry.id]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val listUiState: StateFlow<VocabularyListUiState> = combine(
        allCombined,
        searchQuery,
        selectedFilter,
        connectivityObserver.isOnlineFlow
    ) { all, query, filter, isOnline ->
        val now = System.currentTimeMillis()
        val dueTotal = all.count { SpacedRepetitionEngine.isDueToday(it.reviewState, now) }

        val byFilter = when (filter) {
            VocabularyFilter.ALL -> all
            VocabularyFilter.DUE_TODAY -> all.filter { SpacedRepetitionEngine.isDueToday(it.reviewState, now) }
            VocabularyFilter.NEW -> all.filter { (it.reviewState?.repetitionCount ?: 0) == 0 && it.reviewState?.lastReviewedAtEpochMillis == null }
            VocabularyFilter.FAVORITES -> all.filter { it.entry.favorite }
        }

        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            byFilter
        } else {
            byFilter.filter { item ->
                item.entry.expression.lowercase().contains(q) ||
                    item.entry.meaningTr.lowercase().contains(q) ||
                    item.entry.definitionEn.lowercase().contains(q) ||
                    item.entry.tags.any { it.lowercase().contains(q) } ||
                    item.contexts.any {
                        it.sentence.lowercase().contains(q) || it.sourceTitle.lowercase().contains(q)
                    }
            }
        }

        VocabularyListUiState(
            isLoading = false,
            isDemoMode = userDataRepository.isDemoMode,
            isOnline = isOnline,
            searchQuery = query,
            selectedFilter = filter,
            items = filtered,
            dueCount = dueTotal,
            totalCount = all.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VocabularyListUiState(isDemoMode = userDataRepository.isDemoMode)
    )

    private val _flashcardState = MutableStateFlow(FlashcardSessionUiState())
    val flashcardState: StateFlow<FlashcardSessionUiState> = _flashcardState.asStateFlow()

    private val _aiState = MutableStateFlow(AiInsightUiState())
    val aiState: StateFlow<AiInsightUiState> = _aiState.asStateFlow()

    private var sessionStartedAtMillis: Long = 0L

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onFilterChange(filter: VocabularyFilter) {
        selectedFilter.value = filter
    }

    fun getWordItem(vocabId: String): VocabularyWithContext? {
        return allCombined.value.firstOrNull { it.entry.id == vocabId }
    }

    fun observeAllCombined(): StateFlow<List<VocabularyWithContext>> = allCombined

    suspend fun saveNewWord(
        expression: String,
        meaningTr: String,
        definitionEn: String,
        partOfSpeech: String,
        personalNote: String,
        tagsRaw: String,
        contextSentence: String,
        sourceTitle: String,
        sourceType: ContextSourceType
    ): Result<String> {
        val tags = tagsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(10)
        return userDataRepository.saveVocabularyWithOptionalContext(
            expression = expression,
            meaningTr = meaningTr,
            definitionEn = definitionEn,
            partOfSpeech = partOfSpeech,
            personalNote = personalNote,
            tags = tags,
            contextSentence = contextSentence,
            sourceTitle = sourceTitle,
            sourceType = sourceType
        )
    }

    suspend fun updateWord(entry: VocabularyEntry): Result<Unit> {
        return userDataRepository.updateVocabulary(entry)
    }

    fun toggleFavorite(vocabId: String, favorite: Boolean) {
        viewModelScope.launch {
            userDataRepository.toggleFavorite(vocabId, favorite)
        }
    }

    suspend fun addContext(
        vocabId: String,
        sentence: String,
        sourceTitle: String,
        sourceType: ContextSourceType
    ): Result<String> {
        return userDataRepository.addContextExample(
            vocabularyId = vocabId,
            sentence = sentence,
            sourceTitle = sourceTitle,
            sourceType = sourceType
        )
    }

    suspend fun deleteWord(vocabId: String): Result<Unit> {
        return userDataRepository.deleteVocabularyAndRelated(vocabId)
    }

    fun requestAiInsight(expression: String, contextSentence: String?) {
        viewModelScope.launch {
            _aiState.value = AiInsightUiState(isLoading = true)
            val result = aiWordHelperRepository.generateWordInsight(expression, contextSentence)
            result.onSuccess { insight ->
                _aiState.value = AiInsightUiState(isLoading = false, insight = insight)
            }.onFailure { err ->
                _aiState.value = AiInsightUiState(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "AI suggestion failed"
                )
            }
        }
    }

    fun clearAiInsight() {
        _aiState.value = AiInsightUiState()
    }

    fun startFlashcardSession(includeAllIfNoneDue: Boolean = false) {
        val now = System.currentTimeMillis()
        val all = allCombined.value
        val due = all.filter { SpacedRepetitionEngine.isDueToday(it.reviewState, now) }
        val queue = when {
            due.isNotEmpty() -> due
            includeAllIfNoneDue -> all.take(15)
            else -> emptyList()
        }
        sessionStartedAtMillis = now
        _flashcardState.value = FlashcardSessionUiState(
            isActive = true,
            queue = queue,
            currentIndex = 0,
            isAnswerRevealed = false,
            reviewedCount = 0,
            recalledCount = 0,
            againCount = 0,
            isCompleted = queue.isEmpty(),
            isSubmitting = false,
            errorMessage = null
        )
    }

    fun revealAnswer() {
        _flashcardState.update { it.copy(isAnswerRevealed = true) }
    }

    fun rateCurrentCard(rating: ReviewRating) {
        val state = _flashcardState.value
        val currentItem = state.queue.getOrNull(state.currentIndex) ?: return
        if (state.isSubmitting) return

        viewModelScope.launch {
            _flashcardState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val now = System.currentTimeMillis()
            val eventId = "re_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val res = userDataRepository.submitFlashcardReview(
                vocabularyId = currentItem.entry.id,
                rating = rating,
                eventId = eventId,
                nowEpochMillis = now
            )
            res.onSuccess {
                val nextIndex = state.currentIndex + 1
                val newReviewed = state.reviewedCount + 1
                val newRecalled = state.recalledCount + if (rating != ReviewRating.AGAIN) 1 else 0
                val newAgain = state.againCount + if (rating == ReviewRating.AGAIN) 1 else 0
                val done = nextIndex >= state.queue.size

                _flashcardState.value = state.copy(
                    currentIndex = nextIndex,
                    isAnswerRevealed = false,
                    reviewedCount = newReviewed,
                    recalledCount = newRecalled,
                    againCount = newAgain,
                    isCompleted = done,
                    isSubmitting = false
                )

                if (done && newReviewed > 0) {
                    val elapsedSec = ((now - sessionStartedAtMillis) / 1000L).toInt().coerceAtLeast(1)
                    val session = StudySession(
                        id = "ss_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                        moduleType = StudyModuleType.FLASHCARD,
                        topicOrContentId = "flashcard_srs",
                        durationSeconds = elapsedSec,
                        totalItems = newReviewed,
                        correctCount = newRecalled,
                        dateKey = PracticeScoringEngine.dateKeyFor(now),
                        startedAtEpochMillis = sessionStartedAtMillis,
                        completedAtEpochMillis = now
                    )
                    userDataRepository.recordPracticeCompletion(session, emptyList())
                }
            }.onFailure { err ->
                _flashcardState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = err.localizedMessage ?: "Review save failed"
                    )
                }
            }
        }
    }

    class Factory(
        private val userDataRepository: UserDataRepository,
        private val connectivityObserver: ConnectivityObserver,
        private val aiWordHelperRepository: AiWordHelperRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VocabularyViewModel(
                userDataRepository,
                connectivityObserver,
                aiWordHelperRepository
            ) as T
        }
    }
}
