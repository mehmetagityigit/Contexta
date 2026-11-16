package com.example.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.ContextaEmptyView
import com.example.core.designsystem.ContextaLoadingView
import com.example.core.designsystem.DemoModeBanner
import com.example.core.designsystem.StatusPill
import com.example.feature.auth.examLabel
import com.example.feature.overlay.OverlayPermissionHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartFlashcards: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenVocabDetail: (String) -> Unit,
    onOpenVoiceCapture: () -> Unit,
    onOpenGrammar: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenListening: () -> Unit,
    onOpenLastStudy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlayPerm by remember {
        mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPerm = OverlayPermissionHelper.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        OverlayPermissionHelper.startFloatingCapture(context)
    }

    if (state.isLoading) {
        ContextaLoadingView(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isDemoMode) {
            item {
                DemoModeBanner(modifier = Modifier.widthIn(max = 600.dp))
            }
        }

        if (!state.isOnline) {
            item {
                StatusPill(
                    label = stringResource(R.string.offline_notice),
                    icon = Icons.Outlined.CloudOff,
                    isWarning = true,
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                )
            }
        }

        // Daily summary & primary SRS CTA
        item {
            ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_today_summary),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(label = examLabel(state.settings.targetExam))
                        StatusPill(label = state.settings.cefrLevel.code)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                val goal = state.settings.dailyGoalMinutes.coerceAtLeast(1)
                val progress = (state.todayMinutes.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.home_daily_progress_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.home_daily_progress_value,
                            state.todayMinutes,
                            state.settings.dailyGoalMinutes
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.dueFlashcardsCount > 0) {
                                stringResource(R.string.home_due_cards_title, state.dueFlashcardsCount)
                            } else {
                                stringResource(R.string.home_due_cards_none)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.home_total_words_subtitle, state.totalWordsCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onStartFlashcards,
                        enabled = state.totalWordsCount > 0,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("home_start_review_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.home_start_review))
                    }
                }
            }
        }

        // Continue last study card
        if (!state.lastStudyRoute.isNullOrBlank()) {
            item {
                val route = state.lastStudyRoute!!
                ContextaCard(
                    onClick = { onOpenLastStudy(route) },
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .testTag("home_continue_last_study_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_continue_study_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.home_continue_study_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = stringResource(R.string.home_continue_study_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Quick Module Shortcuts
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_quick_shortcuts),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenVoiceCapture,
                        modifier = Modifier.height(48.dp).testTag("home_voice_capture_button")
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_shortcut_voice))
                    }
                    FilledTonalButton(
                        onClick = onOpenGrammar,
                        modifier = Modifier.height(48.dp).testTag("home_grammar_shortcut")
                    ) {
                        Icon(Icons.Outlined.Spellcheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.module_grammar))
                    }
                    FilledTonalButton(
                        onClick = onOpenReading,
                        modifier = Modifier.height(48.dp).testTag("home_reading_shortcut")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.module_reading))
                    }
                    FilledTonalButton(
                        onClick = onOpenListening,
                        modifier = Modifier.height(48.dp).testTag("home_listening_shortcut")
                    ) {
                        Icon(Icons.Outlined.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.module_listening))
                    }
                }
            }
        }

        // Floating Capture Overlay Card
        item {
            ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BubbleChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_overlay_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when {
                                    !hasOverlayPerm -> stringResource(R.string.home_overlay_perm_missing)
                                    state.overlayRunning -> stringResource(R.string.home_overlay_running)
                                    else -> stringResource(R.string.home_overlay_ready)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!hasOverlayPerm) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(OverlayPermissionHelper.buildOverlayPermissionIntent(context))
                            },
                            modifier = Modifier.height(48.dp).testTag("home_overlay_permission_button")
                        ) {
                            Text(stringResource(R.string.home_overlay_grant_perm))
                        }
                    } else if (state.overlayRunning) {
                        OutlinedButton(
                            onClick = { OverlayPermissionHelper.stopFloatingCapture(context) },
                            modifier = Modifier.height(48.dp).testTag("home_overlay_stop_button")
                        ) {
                            Text(stringResource(R.string.home_overlay_stop))
                        }
                    } else {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    OverlayPermissionHelper.startFloatingCapture(context)
                                }
                            },
                            modifier = Modifier.height(48.dp).testTag("home_overlay_start_button")
                        ) {
                            Text(stringResource(R.string.home_overlay_start))
                        }
                    }
                }
            }
        }

        // Recent Words Header
        item {
            Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_recent_words_title),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    onClick = onOpenVocabulary,
                    modifier = Modifier.height(48.dp).testTag("home_view_all_words_button")
                ) {
                    Text(stringResource(R.string.action_see_all))
                }
            }
        }

        if (state.recentWords.isEmpty()) {
            item {
                ContextaEmptyView(
                    icon = Icons.Outlined.AutoStories,
                    title = stringResource(R.string.vocab_empty_title),
                    subtitle = stringResource(R.string.vocab_empty_subtitle),
                    actionLabel = stringResource(R.string.vocab_add_word),
                    onAction = onOpenVocabulary,
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
        } else {
            items(state.recentWords, key = { it.entry.id }) { item ->
                ContextaCard(
                    onClick = { onOpenVocabDetail(item.entry.id) },
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .testTag("home_recent_word_${item.entry.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.entry.expression,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (item.entry.partOfSpeech.isNotBlank()) {
                            StatusPill(label = item.entry.partOfSpeech)
                        }
                    }
                    if (item.entry.meaningTr.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.entry.meaningTr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val firstCtx = item.contexts.firstOrNull()
                    if (firstCtx != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${firstCtx.sentence}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
