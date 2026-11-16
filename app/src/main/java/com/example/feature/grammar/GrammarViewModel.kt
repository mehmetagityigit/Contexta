package com.example.feature.grammar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.ContentRepository
import com.example.core.data.SettingsRepository
import com.example.core.data.UserDataRepository
import com.example.core.model.AppLanguage
import com.example.core.model.GrammarTopic
import com.example.core.model.PracticeScoringEngine
import com.example.core.model.QuestionAttempt
import com.example.core.model.StudyModuleType
import com.example.core.model.StudySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class GrammarTopicItem(
    val topic: GrammarTopic,
    val completionPercent: Int,
    val lastAccuracyPercent: Int?
)

data class GrammarListUiState(
    val isLoading: Boolean = true,
    val selectedLevel: String = "ALL",
    val language: AppLanguage = AppLanguage.TURKISH,
    val topics: List<GrammarTopicItem> = emptyList(),
    val errorMessage: String? = null
)

class GrammarViewModel(
    private val contentRepository: ContentRepository,
    private val userDataRepository: UserDataRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val rawTopics = MutableStateFlow<List<GrammarTopic>>(emptyList())
    private val loadError = MutableStateFlow<String?>(null)
    private val isLoaded = MutableStateFlow(false)
    private val levelFilter = MutableStateFlow("ALL")

    init {
        loadTopics()
    }

    fun loadTopics() {
        viewModelScope.launch {
            isLoaded.value = false
            loadError.value = null
            runCatching { contentRepository.getGrammarTopics() }
                .onSuccess {
                    rawTopics.value = it
                    isLoaded.value = true
                }
                .onFailure { e ->
                    loadError.value = e.localizedMessage ?: "Failed to load grammar topics"
                    isLoaded.value = true
                }
        }
    }

    fun setLevelFilter(level: String) {
        levelFilter.value = level
    }

    val listUiState: StateFlow<GrammarListUiState> = combine(
        rawTopics,
        isLoaded,
        loadError,
        levelFilter,
        combine(userDataRepository.observeAttempts(), settingsRepository.settingsFlow) { attempts, settings ->
            attempts to settings.language
        }
    ) { topics, loaded, err, level, (attempts, lang) ->
        val filtered = if (level == "ALL") topics else topics.filter { it.level.equals(level, ignoreCase = true) }
        val grammarAttempts = attempts.filter { it.moduleType == StudyModuleType.GRAMMAR }

        val mapped = filtered.map { topic ->
            val topicAttempts = grammarAttempts.filter { it.contentId == topic.id || it.topicId == topic.id }
            val uniqueQuestionsAnswered = topicAttempts.map { it.questionId }.distinct().size
            val completion = if (topic.questions.isEmpty()) 0 else {
                ((uniqueQuestionsAnswered.toDouble() / topic.questions.size.toDouble()) * 100.0)
                    .toInt().coerceIn(0, 100)
            }
            val latestByQ = topicAttempts.groupBy { it.questionId }.mapValues { (_, list) ->
                list.maxByOrNull { it.answeredAtEpochMillis }
            }.values.filterNotNull()
            val accuracy = if (latestByQ.isEmpty()) null else {
                PracticeScoringEngine.accuracyPercent(latestByQ.count { it.isCorrect }, latestByQ.size)
            }
            GrammarTopicItem(
                topic = topic,
                completionPercent = completion,
                lastAccuracyPercent = accuracy
            )
        }

        GrammarListUiState(
            isLoading = !loaded,
            selectedLevel = level,
            language = lang,
            topics = mapped,
            errorMessage = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GrammarListUiState()
    )

    fun saveLastRoute(topicId: String) {
        viewModelScope.launch {
            settingsRepository.saveLastStudyRoute("grammar/$topicId")
        }
    }

    suspend fun recordCompletion(
        topic: GrammarTopic,
        selectedAnswers: Map<String, String>,
        startedAtMillis: Long
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        val durationSec = ((now - startedAtMillis) / 1000L).toInt().coerceAtLeast(1)
        val attempts = topic.questions.mapNotNull { q ->
            val chosen = selectedAnswers[q.id] ?: return@mapNotNull null
            QuestionAttempt(
                id = "ga_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                moduleType = StudyModuleType.GRAMMAR,
                contentId = topic.id,
                questionId = q.id,
                questionType = q.questionType.ifBlank { topic.title },
                topicId = topic.id,
                selectedOptionId = chosen,
                correctOptionId = q.correctOptionId,
                isCorrect = PracticeScoringEngine.scoreQuestion(chosen, q.correctOptionId),
                answeredAtEpochMillis = now
            )
        }
        val correctCount = attempts.count { it.isCorrect }
        val session = StudySession(
            id = "gs_${UUID.randomUUID().toString().replace("-", "").take(14)}",
            moduleType = StudyModuleType.GRAMMAR,
            topicOrContentId = topic.id,
            durationSeconds = durationSec,
            totalItems = attempts.size,
            correctCount = correctCount,
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
            return GrammarViewModel(contentRepository, userDataRepository, settingsRepository) as T
        }
    }
}
