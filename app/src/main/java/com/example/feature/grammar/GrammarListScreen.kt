package com.example.feature.grammar

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.core.model.AppLanguage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GrammarListScreen(
    viewModel: GrammarViewModel,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listUiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_grammar)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("grammar_list_back")) {
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
                onRetry = viewModel::loadTopics,
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
                            listOf("ALL", "B1", "B2", "C1", "C2").forEach { lvl ->
                                FilterChip(
                                    selected = state.selectedLevel == lvl,
                                    onClick = { viewModel.setLevelFilter(lvl) },
                                    label = {
                                        Text(
                                            if (lvl == "ALL") stringResource(R.string.vocab_filter_all) else lvl
                                        )
                                    },
                                    modifier = Modifier.testTag("grammar_filter_$lvl")
                                )
                            }
                        }
                    }

                    items(state.topics, key = { it.topic.id }) { item ->
                        val topic = item.topic
                        val subtitle = if (state.language == AppLanguage.ENGLISH && topic.summaryEn.isNotBlank()) {
                            topic.summaryEn
                        } else {
                            topic.summaryTr
                        }
                        ContextaCard(
                            onClick = { onOpenTopic(topic.id) },
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .testTag("grammar_topic_${topic.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${topic.orderIndex}. ${topic.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusPill(label = topic.level)
                            }
                            if (topic.titleTr.isNotBlank()) {
                                Text(
                                    text = topic.titleTr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.grammar_completion_pct, item.completionPercent),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                item.lastAccuracyPercent?.let { acc ->
                                    Text(
                                        text = stringResource(R.string.grammar_accuracy_pct, acc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { item.completionPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
