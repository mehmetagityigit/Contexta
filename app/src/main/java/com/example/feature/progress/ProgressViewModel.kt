package com.example.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.UserDataRepository
import com.example.core.model.PracticeScoringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val isLoading: Boolean = true,
    val isDemoMode: Boolean = false,
    val daysWindow: Int = 7,
    val summary: PracticeScoringEngine.ProgressSummary? = null,
    val hasAnyActivity: Boolean = false
)

class ProgressViewModel(
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    private val daysWindow = MutableStateFlow(7)

    val uiState: StateFlow<ProgressUiState> = combine(
        daysWindow,
        userDataRepository.observeStudySessions(),
        userDataRepository.observeAttempts(),
        userDataRepository.observeReviewEvents(),
        userDataRepository.observeVocabulary()
    ) { window, sessions, attempts, reviews, vocab ->
        val now = System.currentTimeMillis()
        val summary = PracticeScoringEngine.computeProgress(
            daysWindow = window,
            nowEpochMillis = now,
            sessions = sessions,
            attempts = attempts,
            reviewEvents = reviews,
            vocabulary = vocab
        )
        val hasActivity = sessions.isNotEmpty() || attempts.isNotEmpty() || reviews.isNotEmpty()
        ProgressUiState(
            isLoading = false,
            isDemoMode = userDataRepository.isDemoMode,
            daysWindow = window,
            summary = summary,
            hasAnyActivity = hasActivity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState(isDemoMode = userDataRepository.isDemoMode)
    )

    fun setDaysWindow(days: Int) {
        daysWindow.value = if (days == 30) 30 else 7
    }

    class Factory(
        private val userDataRepository: UserDataRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressViewModel(userDataRepository) as T
        }
    }
}
