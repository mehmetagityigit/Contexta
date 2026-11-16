package com.example.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.common.LocaleHelper
import com.example.core.data.SettingsRepository
import com.example.core.data.UserDataRepository
import com.example.core.data.UserDataTransferManager
import com.example.core.firebase.GoogleAuthManager
import com.example.core.model.AppLanguage
import com.example.core.model.CefrLevel
import com.example.core.model.TargetExam
import com.example.core.model.ThemeMode
import com.example.core.model.UserProfile
import com.example.core.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val isDemoMode: Boolean = false,
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val settings: UserSettings = UserSettings(),
    val statusMessage: String? = null,
    val isBusy: Boolean = false
)

class SettingsViewModel(
    private val userDataRepository: UserDataRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val statusMessage = MutableStateFlow<String?>(null)
    private val isBusy = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        userDataRepository.observeUserProfile(),
        statusMessage,
        isBusy
    ) { settings, profile, msg, busy ->
        val fbUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        SettingsUiState(
            isDemoMode = userDataRepository.isDemoMode,
            userEmail = profile?.email ?: fbUser?.email,
            userDisplayName = profile?.displayName ?: fbUser?.displayName,
            settings = settings,
            statusMessage = msg,
            isBusy = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(isDemoMode = userDataRepository.isDemoMode)
    )

    fun updateLanguage(context: Context, language: AppLanguage, onApplied: () -> Unit) {
        viewModelScope.launch {
            LocaleHelper.saveLanguageCode(context, language)
            settingsRepository.updateSettings { it.copy(language = language) }
            val updated = settingsRepository.settingsFlow.first()
            userDataRepository.syncSettingsToCloud(updated)
            onApplied()
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        updateAndSync { it.copy(themeMode = themeMode) }
    }

    fun updateTargetExam(exam: TargetExam) {
        updateAndSync { it.copy(targetExam = exam) }
        syncProfileTargets(exam = exam)
    }

    fun updateCefrLevel(level: CefrLevel) {
        updateAndSync { it.copy(cefrLevel = level) }
        syncProfileTargets(level = level)
    }

    fun updateDailyGoal(minutes: Int) {
        updateAndSync { it.copy(dailyGoalMinutes = minutes) }
        syncProfileTargets(dailyMinutes = minutes)
    }

    fun updateOverlayVisuals(scale: Float, alpha: Float) {
        updateAndSync { it.copy(overlayScale = scale, overlayAlpha = alpha) }
    }

    private fun updateAndSync(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
            val updated = settingsRepository.settingsFlow.first()
            userDataRepository.syncSettingsToCloud(updated)
        }
    }

    private fun syncProfileTargets(
        exam: TargetExam? = null,
        level: CefrLevel? = null,
        dailyMinutes: Int? = null
    ) {
        viewModelScope.launch {
            val s = settingsRepository.settingsFlow.first()
            val fbUser = runCatching { Firebase.auth.currentUser }.getOrNull()
            userDataRepository.saveUserProfile(
                UserProfile(
                    userId = userDataRepository.ownerId,
                    displayName = fbUser?.displayName,
                    email = fbUser?.email,
                    targetExam = exam ?: s.targetExam,
                    cefrLevel = level ?: s.cefrLevel,
                    dailyGoalMinutes = dailyMinutes ?: s.dailyGoalMinutes,
                    onboardingCompleted = s.onboardingCompleted
                )
            )
        }
    }

    suspend fun buildExportJson(): String {
        val vocab = userDataRepository.observeVocabulary().first()
        val contexts = userDataRepository.observeContexts().first()
        val states = userDataRepository.observeReviewStates().first()
        val settings = settingsRepository.settingsFlow.first()
        return UserDataTransferManager.exportToJson(vocab, contexts, states, settings)
    }

    fun importJson(rawJson: String) {
        viewModelScope.launch {
            isBusy.value = true
            val existingIds = userDataRepository.observeVocabulary().first().map { it.id }.toSet()
            val parsed = UserDataTransferManager.parseAndValidateImport(rawJson, existingIds)
            parsed.onSuccess { bundle ->
                val res = userDataRepository.importBundle(bundle)
                res.onSuccess { count ->
                    statusMessage.value = "Imported $count items"
                }.onFailure { err ->
                    statusMessage.value = err.localizedMessage ?: "Import failed"
                }
            }.onFailure { err ->
                statusMessage.value = err.localizedMessage ?: "Invalid JSON"
            }
            isBusy.value = false
        }
    }

    fun signOut(context: Context, onSignedOut: () -> Unit) {
        viewModelScope.launch {
            isBusy.value = true
            if (!userDataRepository.isDemoMode) {
                GoogleAuthManager.signOut(context)
            }
            isBusy.value = false
            onSignedOut()
        }
    }

    fun deleteAllDataAndAccount(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            isBusy.value = true
            val deleteResult = userDataRepository.deleteAllUserData()
            if (deleteResult.isFailure) {
                statusMessage.value = deleteResult.exceptionOrNull()?.localizedMessage
                isBusy.value = false
                return@launch
            }
            if (!userDataRepository.isDemoMode) {
                runCatching { Firebase.auth.currentUser?.delete()?.await() }
                GoogleAuthManager.signOut(context)
            }
            isBusy.value = false
            onComplete()
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    class Factory(
        private val userDataRepository: UserDataRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(userDataRepository, settingsRepository) as T
        }
    }
}
