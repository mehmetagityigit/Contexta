package com.example.feature.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.ContentRepository
import com.example.core.data.SettingsRepository
import com.example.core.data.UserDataRepository
import com.example.core.model.AppLanguage
import com.example.core.model.ContextSourceType
import com.example.core.model.PracticeScoringEngine
import com.example.core.model.QuestionAttempt
import com.example.core.model.ReadingPassage
import com.example.core.model.StudyModuleType
import com.example.core.model.StudySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ReadingPassageItem(
    val passage: ReadingPassage,
    val bestAccuracyPercent: Int?
)

data class ReadingListUiState(
    val isLoading: Boolean = true,
    val selectedLevelGroup: String = "ALL",
    val selectedGenre: String = "ALL",
    val availableGenres: List<String> = emptyList(),
    val language: AppLanguage = AppLanguage.TURKISH,
    val fontSizeSp: Int = 17,
    val passages: List<ReadingPassageItem> = emptyList(),
    val errorMessage: String? = null
)

class ReadingViewModel(
    private val contentRepository: ContentRepository,
    private val userDataRepository: UserDataRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val rawPassages = MutableStateFlow<List<ReadingPassage>>(emptyList())
    private val isLoaded = MutableStateFlow(false)
    private val loadError = MutableStateFlow<String?>(null)
    private val levelGroupFilter = MutableStateFlow("ALL")
    private val genreFilter = MutableStateFlow("ALL")

    init {
        loadPassages()
    }

    fun loadPassages() {
        viewModelScope.launch {
            isLoaded.value = false
            loadError.value = null
            runCatching { contentRepository.getReadingPassages() }
                .onSuccess {
                    rawPassages.value = it
                    isLoaded.value = true
                }
                .onFailure { e ->
                    loadError.value = e.localizedMessage ?: "Failed to load reading passages"
                    isLoaded.value = true
                }
        }
    }

    fun setLevelGroupFilter(levelGroup: String) {
        levelGroupFilter.value = levelGroup
    }

    fun setGenreFilter(genre: String) {
        genreFilter.value = genre
    }

    fun adjustFontSize(delta: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { s ->
                s.copy(readingFontSizeSp = (s.readingFontSizeSp + delta).coerceIn(14, 24))
            }
        }
    }

    fun saveLastRoute(passageId: String) {
        viewModelScope.launch {
            settingsRepository.saveLastStudyRoute("reading/$passageId")
        }
    }

    val listUiState: StateFlow<ReadingListUiState> = combine(
        rawPassages,
        combine(isLoaded, loadError) { loaded, err -> loaded to err },
        combine(levelGroupFilter, genreFilter) { lg, g -> lg to g },
        combine(userDataRepository.observeAttempts(), settingsRepository.settingsFlow) { a, s -> a to s }
    ) { passages, (loaded, err), (lg, genre), (attempts, settings) ->
        val genres = passages.map { it.genre }.distinct()
        val filtered = passages.filter { p ->
            (lg == "ALL" || p.levelGroup == lg) && (genre == "ALL" || p.genre == genre)
        }
        val readingAttempts = attempts.filter { it.moduleType == StudyModuleType.READING }
        val items = filtered.map { passage ->
            val pAttempts = readingAttempts.filter { it.contentId == passage.id }
            val latestByQ = pAttempts.groupBy { it.questionId }.mapValues { (_, list) ->
                list.maxByOrNull { it.answeredAtEpochMillis }
            }.values.filterNotNull()
            val acc = if (latestByQ.isEmpty()) null else {
                PracticeScoringEngine.accuracyPercent(latestByQ.count { it.isCorrect }, latestByQ.size)
            }
            ReadingPassageItem(passage = passage, bestAccuracyPercent = acc)
        }
        ReadingListUiState(
            isLoading = !loaded,
            selectedLevelGroup = lg,
            selectedGenre = genre,
            availableGenres = genres,
            language = settings.language,
            fontSizeSp = settings.readingFontSizeSp,
            passages = items,
            errorMessage = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReadingListUiState()
    )

    suspend fun saveWordFromPassage(
        expression: String,
        meaningTr: String,
        contextSentence: String,
        passageTitle: String
    ): Result<String> {
        return userDataRepository.saveVocabularyWithOptionalContext(
            expression = expression,
            meaningTr = meaningTr,
            contextSentence = contextSentence,
            sourceTitle = passageTitle,
            sourceType = ContextSourceType.READING
        )
    }

    suspend fun recordCompletion(
        passage: ReadingPassage,
        selectedAnswers: Map<String, String>,
        startedAtMillis: Long
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        val durationSec = ((now - startedAtMillis) / 1000L).toInt().coerceAtLeast(1)
        val attempts = passage.questions.mapNotNull { q ->
            val chosen = selectedAnswers[q.id] ?: return@mapNotNull null
            QuestionAttempt(
                id = "ra_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                moduleType = StudyModuleType.READING,
                contentId = passage.id,
                questionId = q.id,
                questionType = q.questionType.ifBlank { passage.genre },
                topicId = passage.id,
                selectedOptionId = chosen,
                correctOptionId = q.correctOptionId,
                isCorrect = PracticeScoringEngine.scoreQuestion(chosen, q.correctOptionId),
                answeredAtEpochMillis = now
            )
        }
        val session = StudySession(
            id = "rs_${UUID.randomUUID().toString().replace("-", "").take(14)}",
            moduleType = StudyModuleType.READING,
            topicOrContentId = passage.id,
            durationSeconds = durationSec,
            totalItems = attempts.size,
            correctCount = attempts.count { it.isCorrect },
            dateKey = PracticeScoringEngine.dateKeyFor(now),
            startedAtEpochMillis = startedAtMillis,
            completedAtEpochMillis = now
        )
        return userDataRepository.recordPracticeCompletion(session, attempts)
    }

    class Factory(
        private val contentRepository: ContentRepository,
        private val userDataRepository: UserDataRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReadingViewModel(contentRepository, userDataRepository, settingsRepository) as T
        }
    }
}
