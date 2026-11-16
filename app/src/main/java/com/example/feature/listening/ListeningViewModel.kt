package com.example.feature.listening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.ContentRepository
import com.example.core.data.SettingsRepository
import com.example.core.data.UserDataRepository
import com.example.core.model.AppLanguage
import com.example.core.model.ContextSourceType
import com.example.core.model.ListeningLesson
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

data class ListeningLessonItem(
    val lesson: ListeningLesson,
    val bestAccuracyPercent: Int?
)

data class ListeningListUiState(
    val isLoading: Boolean = true,
    val selectedType: String = "ALL",
    val language: AppLanguage = AppLanguage.TURKISH,
    val lessons: List<ListeningLessonItem> = emptyList(),
    val errorMessage: String? = null
)

class ListeningViewModel(
    private val contentRepository: ContentRepository,
    private val userDataRepository: UserDataRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val rawLessons = MutableStateFlow<List<ListeningLesson>>(emptyList())
    private val isLoaded = MutableStateFlow(false)
    private val loadError = MutableStateFlow<String?>(null)
    private val typeFilter = MutableStateFlow("ALL")

    init {
        loadLessons()
    }

    fun loadLessons() {
        viewModelScope.launch {
            isLoaded.value = false
            loadError.value = null
            runCatching { contentRepository.getListeningLessons() }
                .onSuccess {
                    rawLessons.value = it
                    isLoaded.value = true
                }
                .onFailure { e ->
                    loadError.value = e.localizedMessage ?: "Failed to load listening lessons"
                    isLoaded.value = true
                }
        }
    }

    fun setTypeFilter(type: String) {
        typeFilter.value = type
    }

    fun saveLastRoute(lessonId: String) {
        viewModelScope.launch {
            settingsRepository.saveLastStudyRoute("listening/$lessonId")
        }
    }

    val listUiState: StateFlow<ListeningListUiState> = combine(
        rawLessons,
        isLoaded,
        loadError,
        typeFilter,
        combine(userDataRepository.observeAttempts(), settingsRepository.settingsFlow) { a, s -> a to s.language }
    ) { lessons, loaded, err, type, (attempts, lang) ->
        val filtered = if (type == "ALL") lessons else lessons.filter { it.contentType == type }
        val listeningAttempts = attempts.filter { it.moduleType == StudyModuleType.LISTENING }
        val items = filtered.map { lesson ->
            val lAttempts = listeningAttempts.filter { it.contentId == lesson.id }
            val latestByQ = lAttempts.groupBy { it.questionId }.mapValues { (_, list) ->
                list.maxByOrNull { it.answeredAtEpochMillis }
            }.values.filterNotNull()
            val acc = if (latestByQ.isEmpty()) null else {
                PracticeScoringEngine.accuracyPercent(latestByQ.count { it.isCorrect }, latestByQ.size)
            }
            ListeningLessonItem(lesson = lesson, bestAccuracyPercent = acc)
        }
        ListeningListUiState(
            isLoading = !loaded,
            selectedType = type,
            language = lang,
            lessons = items,
            errorMessage = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListeningListUiState()
    )

    suspend fun saveWordFromTranscript(
        expression: String,
        meaningTr: String,
        sentence: String,
        lessonTitle: String
    ): Result<String> {
        return userDataRepository.saveVocabularyWithOptionalContext(
            expression = expression,
            meaningTr = meaningTr,
            contextSentence = sentence,
            sourceTitle = lessonTitle,
            sourceType = ContextSourceType.LISTENING
        )
    }

    suspend fun recordCompletion(
        lesson: ListeningLesson,
        selectedAnswers: Map<String, String>,
        startedAtMillis: Long
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        val durationSec = ((now - startedAtMillis) / 1000L).toInt().coerceAtLeast(1)
        val attempts = lesson.questions.mapNotNull { q ->
            val chosen = selectedAnswers[q.id] ?: return@mapNotNull null
            QuestionAttempt(
                id = "la_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                moduleType = StudyModuleType.LISTENING,
                contentId = lesson.id,
                questionId = q.id,
                questionType = q.questionType.ifBlank { lesson.contentType },
                topicId = lesson.id,
                selectedOptionId = chosen,
                correctOptionId = q.correctOptionId,
                isCorrect = PracticeScoringEngine.scoreQuestion(chosen, q.correctOptionId),
                answeredAtEpochMillis = now
            )
        }
        val session = StudySession(
            id = "ls_${UUID.randomUUID().toString().replace("-", "").take(14)}",
            moduleType = StudyModuleType.LISTENING,
            topicOrContentId = lesson.id,
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
            return ListeningViewModel(contentRepository, userDataRepository, settingsRepository) as T
        }
    }
}
