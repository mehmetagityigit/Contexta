package com.example.feature.listening

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.ContextaErrorView
import com.example.core.designsystem.ContextaLoadingView
import com.example.core.designsystem.StatusPill

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListeningListScreen(
    viewModel: ListeningViewModel,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listUiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_listening)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("listening_list_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> ContextaLoadingView(modifier = Modifier.padding(innerPadding))
            state.errorMessage != null -> ContextaErrorView(
                message = state.errorMessage.orEmpty(),
                onRetry = viewModel::loadLessons,
                modifier = Modifier.padding(innerPadding)
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        FlowRow(
                            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL", "ACADEMIC_LECTURE", "DIALOGUE", "SHORT_STORY").forEach { type ->
                                val label = when (type) {
                                    "ALL" -> stringResource(R.string.vocab_filter_all)
                                    "ACADEMIC_LECTURE" -> stringResource(R.string.listening_type_lecture)
                                    "DIALOGUE" -> stringResource(R.string.listening_type_dialogue)
                                    else -> stringResource(R.string.listening_type_story)
                                }
                                FilterChip(
                                    selected = state.selectedType == type,
                                    onClick = { viewModel.setTypeFilter(type) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("listening_filter_$type")
                                )
                            }
                        }
                    }

                    items(state.lessons, key = { it.lesson.id }) { item ->
                        val l = item.lesson
                        ContextaCard(
                            onClick = { onOpenLesson(l.id) },
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .testTag("listening_lesson_${l.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPill(label = l.level)
                                    StatusPill(label = l.contentType.replace("_", " "))
                                }
                                item.bestAccuracyPercent?.let { acc ->
                                    StatusPill(
                                        label = stringResource(R.string.grammar_accuracy_pct, acc),
                                        isPositive = acc >= 75
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(text = l.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.listening_questions_count, l.questions.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
