package com.example.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.ContextaEmptyView
import com.example.core.designsystem.ContextaLoadingView
import com.example.core.designsystem.DemoModeBanner
import com.example.core.designsystem.StatusPill
import com.example.core.model.PracticeScoringEngine

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        ContextaLoadingView(modifier = modifier)
        return
    }

    val summary = state.summary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isDemoMode) {
            item { DemoModeBanner(modifier = Modifier.widthIn(max = 600.dp)) }
        }

        item {
            Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nav_progress),
                    style = MaterialTheme.typography.headlineMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.daysWindow == 7,
                        onClick = { viewModel.setDaysWindow(7) },
                        label = { Text(stringResource(R.string.progress_7_days)) },
                        modifier = Modifier.testTag("progress_filter_7d")
                    )
                    FilterChip(
                        selected = state.daysWindow == 30,
                        onClick = { viewModel.setDaysWindow(30) },
                        label = { Text(stringResource(R.string.progress_30_days)) },
                        modifier = Modifier.testTag("progress_filter_30d")
                    )
                }
            }
        }

        if (!state.hasAnyActivity || summary == null) {
            item {
                ContextaEmptyView(
                    icon = Icons.Outlined.BarChart,
                    title = stringResource(R.string.progress_empty_title),
                    subtitle = stringResource(R.string.progress_empty_subtitle),
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
            return@LazyColumn
        }

        // Top KPI Summary Row
        item {
            Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    value = "${summary.totalStudyMinutes}",
                    label = stringResource(R.string.progress_minutes_label),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    value = "${summary.reviewedCardsCount}",
                    label = stringResource(R.string.progress_reviews_label),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    value = "${summary.newWordsCount}",
                    label = stringResource(R.string.progress_new_words_label),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calm Canvas Daily Minutes Bar Chart
        item {
            ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.progress_daily_chart_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                DailyMinutesBarChart(
                    points = summary.dailyMinutes,
                    modifier = Modifier.fillMaxWidth().height(132.dp)
                )
            }
        }

        // Module Accuracy Card
        item {
            ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.progress_accuracy_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                AccuracyRow(stringResource(R.string.module_grammar), summary.grammarAccuracy)
                Spacer(Modifier.height(8.dp))
                AccuracyRow(stringResource(R.string.module_reading), summary.readingAccuracy)
                Spacer(Modifier.height(8.dp))
                AccuracyRow(stringResource(R.string.module_listening), summary.listeningAccuracy)
            }
        }

        // Weak Areas
        item {
            Text(
                text = stringResource(R.string.progress_weak_areas_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            )
        }

        if (summary.weakAreas.isEmpty()) {
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.progress_no_weak_areas),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(summary.weakAreas, key = { "${it.moduleType}_${it.key}" }) { weak ->
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StatusPill(label = weak.moduleType.code)
                            Spacer(Modifier.height(4.dp))
                            Text(text = weak.key, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = stringResource(
                                    R.string.progress_attempts_ratio,
                                    weak.correctAttempts,
                                    weak.totalAttempts
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            label = "${weak.accuracyPercent}%",
                            isWarning = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    ContextaCard(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AccuracyRow(
    label: String,
    accuracy: PracticeScoringEngine.ModuleAccuracy
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        val text = if (accuracy.accuracyPercent == null) {
            "—"
        } else {
            "${accuracy.accuracyPercent}% (${accuracy.correctAttempts}/${accuracy.totalAttempts})"
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DailyMinutesBarChart(
    points: List<PracticeScoringEngine.DailyStudyPoint>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val maxMinutes = (points.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(10)
        val count = points.size
        val slotWidth = size.width / count.toFloat()
        val barWidth = (slotWidth * 0.56f).coerceIn(4f, 36f)
        val heightPx = size.height

        points.forEachIndexed { idx, point ->
            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, heightPx),
                cornerRadius = CornerRadius(6f, 6f)
            )
            if (point.minutes > 0) {
                val ratio = (point.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.08f, 1f)
                val barHeight = heightPx * ratio
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, heightPx - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
    }
}
