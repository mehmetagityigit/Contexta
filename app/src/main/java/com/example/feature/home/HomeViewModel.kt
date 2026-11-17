package com.example.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.common.ConnectivityObserver
import com.example.core.data.AppContainer
import com.example.core.data.SettingsRepository
import com.example.core.data.UserDataRepository
import com.example.core.model.PracticeScoringEngine
import com.example.core.model.SpacedRepetitionEngine
import com.example.core.model.UserSettings
import com.example.core.model.VocabularyWithContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val isDemoMode: Boolean = false,
    val isOnline: Boolean = true,
    val displayName: String? = null,
    val settings: UserSettings = UserSettings(),
    val todayMinutes: Int = 0,
    val dueFlashcardsCount: Int = 0,
    val masteredWordsCount: Int = 0,
    val totalWordsCount: Int = 0,
    val weeklyDailyMinutes: List<PracticeScoringEngine.DailyStudyPoint> = emptyList(),
    val recentWords: List<VocabularyWithContext> = emptyList(),
    val lastStudyRoute: String? = null,
    val overlayRunning: Boolean = false
)

class HomeViewModel(
    private val userDataRepository: UserDataRepository,
    settingsRepository: SettingsRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private data class CoreFlows(
        val settings: UserSettings,
        val lastRoute: String?,
        val isOnline: Boolean,
        val overlayRunning: Boolean,
        val profileDisplayName: String?
    )

    private val coreFlows = combine(
        settingsRepository.settingsFlow,
        settingsRepository.lastStudyRouteFlow,
        connectivityObserver.isOnlineFlow,
        AppContainer.overlayRunning,
        userDataRepository.observeUserProfile()
    ) { settings, lastRoute, isOnline, overlayRunning, profile ->
        CoreFlows(settings, lastRoute, isOnline, overlayRunning, profile?.displayName)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        coreFlows,
        userDataRepository.observeVocabulary(),
        userDataRepository.observeContexts(),
        userDataRepository.observeReviewStates(),
        userDataRepository.observeStudySessions()
    ) { core, vocab, contexts, reviewStates, sessions ->
        val now = System.currentTimeMillis()
        val todayKey = PracticeScoringEngine.dateKeyFor(now)
        val todaySec = sessions.filter { it.dateKey == todayKey }.sumOf { it.durationSeconds.coerceAtLeast(0) }
        val todayMins = if (todaySec == 0) 0 else maxOf(1, (todaySec + 30) / 60)

        val statesById = reviewStates.associateBy { it.vocabularyId }
        val contextsByVocab = contexts.groupBy { it.vocabularyId }

        val dueCount = vocab.count { entry ->
            SpacedRepetitionEngine.isDueToday(statesById[entry.id], now)
        }
        val masteredCount = vocab.count { entry ->
            val rs = statesById[entry.id]
            rs != null && rs.repetitionCount >= 2 && rs.intervalDays >= 6
        }

        val weeklyProgress = PracticeScoringEngine.computeProgress(
            daysWindow = 7,
            nowEpochMillis = now,
            sessions = sessions,
            attempts = emptyList(),
            reviewEvents = emptyList(),
            vocabulary = vocab
        )

        val recent = vocab
            .sortedByDescending { it.updatedAtEpochMillis }
            .take(5)
            .map { entry ->
                VocabularyWithContext(
                    entry = entry,
                    contexts = contextsByVocab[entry.id].orEmpty(),
                    reviewState = statesById[entry.id]
                )
            }

        HomeUiState(
            isLoading = false,
            isDemoMode = userDataRepository.isDemoMode,
            isOnline = core.isOnline,
            displayName = core.profileDisplayName,
            settings = core.settings,
            todayMinutes = todayMins,
            dueFlashcardsCount = dueCount,
            masteredWordsCount = masteredCount,
            totalWordsCount = vocab.size,
            weeklyDailyMinutes = weeklyProgress.dailyMinutes,
            recentWords = recent,
            lastStudyRoute = core.lastRoute,
            overlayRunning = core.overlayRunning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isDemoMode = userDataRepository.isDemoMode)
    )

    class Factory(
        private val userDataRepository: UserDataRepository,
        private val settingsRepository: SettingsRepository,
        private val connectivityObserver: ConnectivityObserver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(userDataRepository, settingsRepository, connectivityObserver) as T
        }
    }
}
