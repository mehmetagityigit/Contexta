package com.example.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.core.designsystem.LocalContextaColors
import com.example.core.designsystem.StatusPill
import com.example.core.model.PracticeScoringEngine
import com.example.feature.auth.examLabel
import com.example.feature.overlay.OverlayPermissionHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    val ext = LocalContextaColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Greeting Header (avatar + greeting + compact pills)
        item {
            val firstName = state.displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ext.navySurface,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = null,
                                tint = ext.onNavySurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (firstName != null) {
                                stringResource(R.string.home_greeting_user, firstName)
                            } else {
                                stringResource(R.string.home_greeting_default)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.home_greeting_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.minutes_format, state.todayMinutes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (state.isDemoMode) {
                                stringResource(R.string.sync_badge_demo)
                            } else {
                                examLabel(state.settings.targetExam)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

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

        // 2. Midnight Navy Hero Banner Card (matching screenshot hero gradient + dual CTAs)
        item {
            val heroShape = MaterialTheme.shapes.large
            OutlinedCard(
                shape = heroShape,
                border = BorderStroke(1.dp, ext.navyBorder),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.Transparent,
                    contentColor = ext.onNavySurface
                ),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = heroShape,
                        ambientColor = ext.cardShadow,
                        spotColor = ext.cardShadow
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(ext.navySurface, ext.navySurfaceEnd)
                            )
                        )
                ) {
                    // Subtle clean architectural arc lines on the right (no sparkles/AI art)
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val cx = size.width * 0.88f
                        val cy = size.height * 0.42f
                        val ringColor = ext.heroAccentLine.copy(alpha = 0.42f)
                        drawCircle(
                            color = ringColor,
                            radius = size.minDimension * 0.42f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = ringColor,
                            radius = size.minDimension * 0.68f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ext.navyPillBg
                        ) {
                            Text(
                                text = stringResource(R.string.home_hero_kicker),
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.onNavyMuted,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.home_hero_headline),
                            style = MaterialTheme.typography.headlineSmall,
                            color = ext.onNavySurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (state.dueFlashcardsCount > 0) {
                                stringResource(R.string.home_due_cards_title, state.dueFlashcardsCount)
                            } else {
                                stringResource(R.string.home_due_cards_none)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.onNavyMuted
                        )

                        Spacer(Modifier.height(10.dp))
                        val goal = state.settings.dailyGoalMinutes.coerceAtLeast(1)
                        val dailyProgress = (state.todayMinutes.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { dailyProgress },
                            color = ext.navyProgressFill,
                            trackColor = ext.navyTrackBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenVocabulary,
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.navyActionBg,
                                    contentColor = ext.navyActionText
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_add_word_cta")
                            ) {
                                Text(
                                    text = stringResource(R.string.home_hero_add_word),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = onStartFlashcards,
                                enabled = state.totalWordsCount > 0,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, ext.heroAccentLine),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ext.onNavySurface,
                                    disabledContentColor = ext.onNavyMuted
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_start_review_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.home_hero_practice),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Weekly Learning Chart + 3-Metric Strip White Card
        item {
            ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_weekly_chart_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.home_weekly_chart_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.home_daily_goal_chip, state.settings.dailyGoalMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                WeeklyActivityChart(
                    points = state.weeklyDailyMinutes,
                    goalMinutes = state.settings.dailyGoalMinutes,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = ext.cardBorder)
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HomeKpiItem(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        iconTint = MaterialTheme.colorScheme.onBackground,
                        value = "${state.totalWordsCount}",
                        label = stringResource(R.string.home_metric_total_words),
                        modifier = Modifier.weight(1f)
                    )
                    HomeKpiItem(
                        icon = Icons.Outlined.Schedule,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        value = "${state.dueFlashcardsCount}",
                        label = stringResource(R.string.home_metric_due_now),
                        modifier = Modifier.weight(1f)
                    )
                    HomeKpiItem(
                        icon = Icons.Outlined.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        value = "${state.masteredWordsCount}",
                        label = stringResource(R.string.home_metric_mastered),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Word Capture Tools Section Header
        item {
            Text(
                text = stringResource(R.string.home_tools_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            )
        }

        // Floating Bubble Switch Card
        item {
            val isOverlayActive = hasOverlayPerm && state.overlayRunning
            val toggleTestTag = when {
                !hasOverlayPerm -> "home_overlay_permission_button"
                state.overlayRunning -> "home_overlay_stop_button"
                else -> "home_overlay_start_button"
            }

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
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureInPictureAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.home_overlay_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isOverlayActive) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = if (isOverlayActive) {
                                            stringResource(R.string.home_overlay_on_badge)
                                        } else {
                                            stringResource(R.string.home_overlay_off_badge)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverlayActive) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when {
                                    !hasOverlayPerm -> stringResource(R.string.home_overlay_perm_missing)
                                    else -> stringResource(R.string.home_overlay_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = isOverlayActive,
                        onCheckedChange = { shouldEnable ->
                            if (!hasOverlayPerm) {
                                context.startActivity(OverlayPermissionHelper.buildOverlayPermissionIntent(context))
                            } else if (shouldEnable) {
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
                            } else {
                                OverlayPermissionHelper.stopFloatingCapture(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag(toggleTestTag)
                    )
                }
            }
        }

        // 5. 2-Column Capture & Study Action Cards (mirroring screenshot's twin grid cards)
        item {
            Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContextaCard(
                    onClick = onOpenVoiceCapture,
                    modifier = Modifier.weight(1f).testTag("home_voice_capture_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        StatusPill(label = stringResource(R.string.home_tool_voice_chip))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.home_tool_voice_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_tool_voice_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val lastRoute = state.lastStudyRoute
                ContextaCard(
                    onClick = {
                        if (!lastRoute.isNullOrBlank()) onOpenLastStudy(lastRoute) else onOpenGrammar()
                    },
                    modifier = Modifier.weight(1f).testTag("home_continue_last_study_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        StatusPill(label = state.settings.cefrLevel.code)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (!lastRoute.isNullOrBlank()) {
                            stringResource(R.string.home_continue_study_title)
                        } else {
                            stringResource(R.string.home_tool_study_title)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_tool_study_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 6. Study Module Pills
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_quick_shortcuts),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val shortcutColors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                    val shortcutBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    val shortcutShape = MaterialTheme.shapes.small

                    OutlinedButton(
                        onClick = onOpenGrammar,
                        shape = shortcutShape,
                        colors = shortcutColors,
                        border = shortcutBorder,
                        modifier = Modifier.height(48.dp).testTag("home_grammar_shortcut")
                    ) {
                        Icon(
                            Icons.Outlined.Spellcheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.module_grammar))
                    }
                    OutlinedButton(
                        onClick = onOpenReading,
                        shape = shortcutShape,
                        colors = shortcutColors,
                        border = shortcutBorder,
                        modifier = Modifier.height(48.dp).testTag("home_reading_shortcut")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.module_reading))
                    }
                    OutlinedButton(
                        onClick = onOpenListening,
                        shape = shortcutShape,
                        colors = shortcutColors,
                        border = shortcutBorder,
                        modifier = Modifier.height(48.dp).testTag("home_listening_shortcut")
                    ) {
                        Icon(
                            Icons.Outlined.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.module_listening))
                    }
                }
            }
        }

        // 7. Recent Words Header + Cards
        item {
            Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_recent_words_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = onOpenVocabulary,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
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

@Composable
private fun WeeklyActivityChart(
    points: List<PracticeScoringEngine.DailyStudyPoint>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val ext = LocalContextaColors.current
    val locale = Locale.getDefault()
    val dayLabels = remember(points, locale) {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormatter = SimpleDateFormat("EEE", locale)
        if (points.isEmpty()) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -6)
            (0 until 7).map { idx ->
                if (idx > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
                dayFormatter.format(cal.time).replace(".", "").take(3) to 0
            }
        } else {
            points.map { pt ->
                val date = runCatching { parser.parse(pt.dateKey) }.getOrNull()
                val label = if (date != null) {
                    dayFormatter.format(date).replace(".", "").take(3)
                } else pt.dateKey.takeLast(2)
                label to pt.minutes
            }
        }
    }

    Column(modifier = modifier) {
        val maxVal = maxOf(
            goalMinutes.coerceAtLeast(10),
            dayLabels.maxOfOrNull { it.second } ?: 0
        ).coerceAtLeast(10)

        Canvas(modifier = Modifier.fillMaxWidth().height(84.dp)) {
            val count = dayLabels.size.coerceAtLeast(1)
            val slotWidth = size.width / count.toFloat()
            val barWidth = (slotWidth * 0.48f).coerceIn(14.dp.toPx(), 32.dp.toPx())
            val chartHeight = size.height
            val minPillHeight = 10.dp.toPx()
            val barBrush = Brush.verticalGradient(colors = listOf(ext.barTop, ext.barBottom))

            dayLabels.forEachIndexed { idx, (_, mins) ->
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                val h = if (mins <= 0) {
                    minPillHeight
                } else {
                    (chartHeight * (mins.toFloat() / maxVal.toFloat())).coerceIn(18.dp.toPx(), chartHeight)
                }
                val top = chartHeight - h
                val radius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                if (mins <= 0) {
                    drawRoundRect(
                        color = ext.barBottom.copy(alpha = 0.28f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, h),
                        cornerRadius = radius
                    )
                } else {
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, h),
                        cornerRadius = radius
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEachIndexed { idx, (label, _) ->
                val isToday = idx == dayLabels.lastIndex
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeKpiItem(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
