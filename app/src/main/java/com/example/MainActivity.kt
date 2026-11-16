package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.common.LocaleHelper
import com.example.core.data.AppContainer
import com.example.core.data.UserDataRepository
import com.example.core.designsystem.ContextaLoadingView
import com.example.core.designsystem.ContextaTheme
import com.example.core.firebase.FirestoreUserDataRepository
import com.example.core.firebase.GoogleAuthManager
import com.example.core.firebase.authStateFlow
import com.example.core.firebase.configureAppCheck
import com.example.core.model.UserProfile
import com.example.core.navigation.FlashcardReviewRoute
import com.example.core.navigation.GrammarDetailRoute
import com.example.core.navigation.GrammarListRoute
import com.example.core.navigation.HomeRoute
import com.example.core.navigation.ListeningListRoute
import com.example.core.navigation.ListeningSessionRoute
import com.example.core.navigation.ProgressRoute
import com.example.core.navigation.ReadingListRoute
import com.example.core.navigation.ReadingSessionRoute
import com.example.core.navigation.SettingsRoute
import com.example.core.navigation.StudyHubRoute
import com.example.core.navigation.VocabularyDetailRoute
import com.example.core.navigation.VocabularyRoute
import com.example.core.navigation.VoiceQuickCaptureRoute
import com.example.feature.auth.AuthScreen
import com.example.feature.auth.OnboardingScreen
import com.example.feature.grammar.GrammarDetailScreen
import com.example.feature.grammar.GrammarListScreen
import com.example.feature.grammar.GrammarViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.home.HomeViewModel
import com.example.feature.listening.ListeningListScreen
import com.example.feature.listening.ListeningSessionScreen
import com.example.feature.listening.ListeningViewModel
import com.example.feature.progress.ProgressScreen
import com.example.feature.progress.ProgressViewModel
import com.example.feature.reading.ReadingListScreen
import com.example.feature.reading.ReadingSessionScreen
import com.example.feature.reading.ReadingViewModel
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import com.example.feature.study.StudyHubScreen
import com.example.feature.vocabulary.FlashcardSessionScreen
import com.example.feature.vocabulary.VocabularyDetailScreen
import com.example.feature.vocabulary.VocabularyListScreen
import com.example.feature.vocabulary.VocabularyViewModel
import com.example.feature.vocabulary.VoiceQuickCaptureScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingVoiceIntentState by mutableStateOf<Pair<Boolean, String>?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check BEFORE enableEdgeToEdge / setContent / Firebase calls
        configureAppCheck(this, intent)
        extractVoiceExtras(intent)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val settingsRepo = remember { AppContainer.getSettingsRepository(context) }
            val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.example.core.model.UserSettings()
            )
            val firebaseUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
            val isDemoMode by AppContainer.isDemoMode.collectAsStateWithLifecycle()

            var authBootstrapping by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                if (Firebase.auth.currentUser == null && !isDemoMode) {
                    GoogleAuthManager.attemptSilentAutoSignIn(context)
                }
                authBootstrapping = false
            }

            ContextaTheme(themeMode = settings.themeMode) {
                val scope = rememberCoroutineScope()

                when {
                    authBootstrapping -> {
                        ContextaLoadingView(message = stringResource(R.string.auth_checking_session))
                    }
                    firebaseUser == null && !isDemoMode -> {
                        AuthScreen(
                            onSignedIn = { AppContainer.setDemoMode(false) },
                            onEnterDemoMode = { AppContainer.setDemoMode(true) }
                        )
                    }
                    else -> {
                        val activeUser = firebaseUser
                        val userDataRepo: UserDataRepository = remember(activeUser?.uid, isDemoMode) {
                            if (activeUser != null) {
                                FirestoreUserDataRepository(context.applicationContext, activeUser.uid)
                            } else {
                                AppContainer.getDemoRepository()
                            }
                        }

                        if (!settings.onboardingCompleted) {
                            OnboardingScreen(
                                initialExam = settings.targetExam,
                                initialLevel = settings.cefrLevel,
                                initialDailyMinutes = settings.dailyGoalMinutes,
                                onComplete = { exam, level, mins ->
                                    scope.launch {
                                        settingsRepo.updateSettings { s ->
                                            s.copy(
                                                targetExam = exam,
                                                cefrLevel = level,
                                                dailyGoalMinutes = mins,
                                                onboardingCompleted = true
                                            )
                                        }
                                        userDataRepo.saveUserProfile(
                                            UserProfile(
                                                userId = userDataRepo.ownerId,
                                                displayName = activeUser?.displayName,
                                                email = activeUser?.email,
                                                targetExam = exam,
                                                cefrLevel = level,
                                                dailyGoalMinutes = mins,
                                                onboardingCompleted = true
                                            )
                                        )
                                    }
                                }
                            )
                        } else {
                            ContextaMainShell(
                                userDataRepository = userDataRepo,
                                pendingVoiceRequest = pendingVoiceIntentState,
                                onConsumeVoiceRequest = { pendingVoiceIntentState = null },
                                onSignedOut = { AppContainer.setDemoMode(false) },
                                onSwitchToRealAuth = { AppContainer.setDemoMode(false) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractVoiceExtras(intent)
    }

    private fun extractVoiceExtras(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_VOICE_CAPTURE, false) == true) {
            val expr = intent.getStringExtra(EXTRA_DRAFT_EXPRESSION).orEmpty()
            pendingVoiceIntentState = true to expr
            intent.removeExtra(EXTRA_OPEN_VOICE_CAPTURE)
        }
    }

    companion object {
        const val EXTRA_OPEN_VOICE_CAPTURE = "extra_open_voice_capture"
        const val EXTRA_DRAFT_EXPRESSION = "extra_draft_expression"
    }
}

private data class TopTabItem(
    val route: Any,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)

@Composable
private fun ContextaMainShell(
    userDataRepository: UserDataRepository,
    pendingVoiceRequest: Pair<Boolean, String>?,
    onConsumeVoiceRequest: () -> Unit,
    onSignedOut: () -> Unit,
    onSwitchToRealAuth: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val settingsRepo = remember { AppContainer.getSettingsRepository(context) }
    val contentRepo = remember { AppContainer.getContentRepository(context) }
    val connectivity = remember { AppContainer.getConnectivityObserver(context) }
    val aiHelper = remember { AppContainer.getAiWordHelperRepository() }

    val repoKey = "${userDataRepository.isDemoMode}_${userDataRepository.ownerId}"
    val homeVm: HomeViewModel = viewModel(
        key = "home_$repoKey",
        factory = HomeViewModel.Factory(userDataRepository, settingsRepo, connectivity)
    )
    val vocabVm: VocabularyViewModel = viewModel(
        key = "vocab_$repoKey",
        factory = VocabularyViewModel.Factory(userDataRepository, connectivity, aiHelper)
    )
    val grammarVm: GrammarViewModel = viewModel(
        key = "grammar_$repoKey",
        factory = GrammarViewModel.Factory(contentRepo, userDataRepository, settingsRepo)
    )
    val readingVm: ReadingViewModel = viewModel(
        key = "reading_$repoKey",
        factory = ReadingViewModel.Factory(contentRepo, userDataRepository, settingsRepo)
    )
    val listeningVm: ListeningViewModel = viewModel(
        key = "listening_$repoKey",
        factory = ListeningViewModel.Factory(contentRepo, userDataRepository, settingsRepo)
    )
    val progressVm: ProgressViewModel = viewModel(
        key = "progress_$repoKey",
        factory = ProgressViewModel.Factory(userDataRepository)
    )
    val settingsVm: SettingsViewModel = viewModel(
        key = "settings_$repoKey",
        factory = SettingsViewModel.Factory(userDataRepository, settingsRepo)
    )

    LaunchedEffect(pendingVoiceRequest) {
        val req = pendingVoiceRequest ?: return@LaunchedEffect
        if (req.first) {
            navController.navigate(VoiceQuickCaptureRoute(req.second))
            onConsumeVoiceRequest()
        }
    }

    val topTabs = remember {
        listOf(
            TopTabItem(HomeRoute, R.string.nav_home, Icons.Outlined.Home, "nav_home"),
            TopTabItem(VocabularyRoute, R.string.nav_vocabulary, Icons.Outlined.AutoStories, "nav_vocab"),
            TopTabItem(StudyHubRoute, R.string.nav_study, Icons.Outlined.School, "nav_study"),
            TopTabItem(ProgressRoute, R.string.nav_progress, Icons.Outlined.BarChart, "nav_progress"),
            TopTabItem(SettingsRoute, R.string.nav_settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val isTopLevel = currentDest == null ||
        currentDest.hasRoute<HomeRoute>() ||
        currentDest.hasRoute<VocabularyRoute>() ||
        currentDest.hasRoute<StudyHubRoute>() ||
        currentDest.hasRoute<ProgressRoute>() ||
        currentDest.hasRoute<SettingsRoute>()

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                VocabularyRoute -> currentDest?.hasRoute<VocabularyRoute>() == true
                                StudyHubRoute -> currentDest?.hasRoute<StudyHubRoute>() == true
                                ProgressRoute -> currentDest?.hasRoute<ProgressRoute>() == true
                                SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isTopLevel && useRail) {
                    NavigationRail {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                VocabularyRoute -> currentDest?.hasRoute<VocabularyRoute>() == true
                                StudyHubRoute -> currentDest?.hasRoute<StudyHubRoute>() == true
                                ProgressRoute -> currentDest?.hasRoute<ProgressRoute>() == true
                                SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            viewModel = homeVm,
                            onStartFlashcards = {
                                vocabVm.startFlashcardSession(includeAllIfNoneDue = false)
                                navController.navigate(FlashcardReviewRoute)
                            },
                            onOpenVocabulary = { navigateTopTab(VocabularyRoute) },
                            onOpenVocabDetail = { id -> navController.navigate(VocabularyDetailRoute(id)) },
                            onOpenVoiceCapture = { navController.navigate(VoiceQuickCaptureRoute()) },
                            onOpenGrammar = { navController.navigate(GrammarListRoute) },
                            onOpenReading = { navController.navigate(ReadingListRoute) },
                            onOpenListening = { navController.navigate(ListeningListRoute) },
                            onOpenLastStudy = { routeStr ->
                                when {
                                    routeStr.startsWith("grammar/") -> {
                                        val id = routeStr.removePrefix("grammar/")
                                        navController.navigate(GrammarDetailRoute(id))
                                    }
                                    routeStr.startsWith("reading/") -> {
                                        val id = routeStr.removePrefix("reading/")
                                        navController.navigate(ReadingSessionRoute(id))
                                    }
                                    routeStr.startsWith("listening/") -> {
                                        val id = routeStr.removePrefix("listening/")
                                        navController.navigate(ListeningSessionRoute(id))
                                    }
                                    else -> navigateTopTab(StudyHubRoute)
                                }
                            }
                        )
                    }

                    composable<VocabularyRoute> {
                        VocabularyListScreen(
                            viewModel = vocabVm,
                            onOpenDetail = { id -> navController.navigate(VocabularyDetailRoute(id)) },
                            onStartFlashcards = {
                                vocabVm.startFlashcardSession(includeAllIfNoneDue = false)
                                navController.navigate(FlashcardReviewRoute)
                            },
                            onOpenVoiceCapture = { navController.navigate(VoiceQuickCaptureRoute()) }
                        )
                    }

                    composable<VocabularyDetailRoute> { entry ->
                        val args = entry.toRoute<VocabularyDetailRoute>()
                        VocabularyDetailScreen(
                            vocabId = args.vocabId,
                            viewModel = vocabVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<FlashcardReviewRoute> {
                        FlashcardSessionScreen(
                            viewModel = vocabVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<VoiceQuickCaptureRoute> { entry ->
                        val args = entry.toRoute<VoiceQuickCaptureRoute>()
                        VoiceQuickCaptureScreen(
                            initialExpression = args.initialExpression,
                            viewModel = vocabVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<StudyHubRoute> {
                        StudyHubScreen(
                            onOpenGrammar = { navController.navigate(GrammarListRoute) },
                            onOpenReading = { navController.navigate(ReadingListRoute) },
                            onOpenListening = { navController.navigate(ListeningListRoute) }
                        )
                    }

                    composable<GrammarListRoute> {
                        GrammarListScreen(
                            viewModel = grammarVm,
                            onBack = { navController.popBackStack() },
                            onOpenTopic = { id -> navController.navigate(GrammarDetailRoute(id)) }
                        )
                    }

                    composable<GrammarDetailRoute> { entry ->
                        val args = entry.toRoute<GrammarDetailRoute>()
                        GrammarDetailScreen(
                            topicId = args.topicId,
                            viewModel = grammarVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<ReadingListRoute> {
                        ReadingListScreen(
                            viewModel = readingVm,
                            onBack = { navController.popBackStack() },
                            onOpenPassage = { id -> navController.navigate(ReadingSessionRoute(id)) }
                        )
                    }

                    composable<ReadingSessionRoute> { entry ->
                        val args = entry.toRoute<ReadingSessionRoute>()
                        ReadingSessionScreen(
                            passageId = args.passageId,
                            viewModel = readingVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<ListeningListRoute> {
                        ListeningListScreen(
                            viewModel = listeningVm,
                            onBack = { navController.popBackStack() },
                            onOpenLesson = { id -> navController.navigate(ListeningSessionRoute(id)) }
                        )
                    }

                    composable<ListeningSessionRoute> { entry ->
                        val args = entry.toRoute<ListeningSessionRoute>()
                        ListeningSessionScreen(
                            lessonId = args.lessonId,
                            viewModel = listeningVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<ProgressRoute> {
                        ProgressScreen(viewModel = progressVm)
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            viewModel = settingsVm,
                            onSignedOut = onSignedOut,
                            onSwitchToRealAuth = onSwitchToRealAuth
                        )
                    }
                }
            }
        }
    }
}
