package com.example.core.data

import android.content.Context
import com.example.core.common.ConnectivityObserver
import com.example.core.firebase.FirebaseAiWordHelperRepository
import com.example.core.firebase.FirestoreUserDataRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight dependency holder scoped to the process.
 * Does NOT initialize Firebase App Check or Firestore eagerly on cold start;
 * Firebase instances are resolved lazily only after MainActivity configures App Check
 * and authentication/demo mode is selected.
 */
object AppContainer {
    @Volatile
    private var settingsRepo: SettingsRepository? = null

    @Volatile
    private var contentRepo: ContentRepository? = null

    @Volatile
    private var connectivity: ConnectivityObserver? = null

    @Volatile
    private var demoRepo: DemoUserDataRepository? = null

    private val aiHelperRepo: AiWordHelperRepository by lazy {
        FirebaseAiWordHelperRepository()
    }

    private val _overlayRunning = MutableStateFlow(false)
    val overlayRunning: StateFlow<Boolean> = _overlayRunning.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    fun getSettingsRepository(context: Context): SettingsRepository {
        return settingsRepo ?: synchronized(this) {
            settingsRepo ?: DataStoreSettingsRepository(context.applicationContext).also { settingsRepo = it }
        }
    }

    fun getContentRepository(context: Context): ContentRepository {
        return contentRepo ?: synchronized(this) {
            contentRepo ?: AssetContentRepository(context.applicationContext).also { contentRepo = it }
        }
    }

    fun getConnectivityObserver(context: Context): ConnectivityObserver {
        return connectivity ?: synchronized(this) {
            connectivity ?: ConnectivityObserver(context.applicationContext).also { connectivity = it }
        }
    }

    fun getDemoRepository(): DemoUserDataRepository {
        return demoRepo ?: synchronized(this) {
            demoRepo ?: DemoUserDataRepository().also { demoRepo = it }
        }
    }

    fun getAiWordHelperRepository(): AiWordHelperRepository = aiHelperRepo

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    fun setOverlayRunning(running: Boolean) {
        _overlayRunning.value = running
    }

    /**
     * Resolves the active UserDataRepository for user actions or background overlay quick-capture.
     * Returns null if neither Firebase Auth nor Demo Mode is active.
     */
    fun resolveActiveUserRepository(context: Context): UserDataRepository? {
        val firebaseUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        if (!firebaseUid.isNullOrBlank()) {
            return FirestoreUserDataRepository(context.applicationContext, firebaseUid)
        }
        if (_isDemoMode.value) {
            return getDemoRepository()
        }
        return null
    }
}
