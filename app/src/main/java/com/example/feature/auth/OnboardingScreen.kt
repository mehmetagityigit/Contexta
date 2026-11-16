package com.example.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.model.CefrLevel
import com.example.core.model.TargetExam

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialExam: TargetExam,
    initialLevel: CefrLevel,
    initialDailyMinutes: Int,
    onComplete: (TargetExam, CefrLevel, Int) -> Unit
) {
    var selectedExam by remember { mutableStateOf(initialExam) }
    var selectedLevel by remember { mutableStateOf(initialLevel) }
    var dailyMinutes by remember { mutableIntStateOf(initialDailyMinutes) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                ContextaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.onboarding_goal_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TargetExam.entries.forEach { exam ->
                            FilterChip(
                                selected = selectedExam == exam,
                                onClick = { selectedExam = exam },
                                label = { Text(examLabel(exam)) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_exam_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ContextaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.onboarding_level_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CefrLevel.entries.forEach { level ->
                            FilterChip(
                                selected = selectedLevel == level,
                                onClick = { selectedLevel = level },
                                label = { Text(level.code) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ContextaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.onboarding_daily_goal_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 30, 45).forEach { mins ->
                            FilterChip(
                                selected = dailyMinutes == mins,
                                onClick = { dailyMinutes = mins },
                                label = { Text(stringResource(R.string.minutes_format, mins)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onComplete(selectedExam, selectedLevel, dailyMinutes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_continue_button")
                ) {
                    Text(stringResource(R.string.onboarding_continue))
                }
            }
        }
    }
}

@Composable
fun examLabel(exam: TargetExam): String = when (exam) {
    TargetExam.GENERAL -> stringResource(R.string.exam_general)
    TargetExam.SAT -> stringResource(R.string.exam_sat)
    TargetExam.IELTS -> stringResource(R.string.exam_ielts)
    TargetExam.AP_ACADEMIC -> stringResource(R.string.exam_ap_academic)
}
