package com.example.feature.reading

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
fun ReadingListScreen(
    viewModel: ReadingViewModel,
    onBack: () -> Unit,
    onOpenPassage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listUiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_reading)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reading_list_back")) {
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
                onRetry = viewModel::loadPassages,
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
                            listOf("ALL", "B1-B2", "C1-C2").forEach { lg ->
                                FilterChip(
                                    selected = state.selectedLevelGroup == lg,
                                    onClick = { viewModel.setLevelGroupFilter(lg) },
                                    label = {
                                        Text(if (lg == "ALL") stringResource(R.string.vocab_filter_all) else lg)
                                    },
                                    modifier = Modifier.testTag("reading_level_$lg")
                                )
                            }
                        }
                    }

                    if (state.availableGenres.isNotEmpty()) {
                        item {
                            FlowRow(
                                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = state.selectedGenre == "ALL",
                                    onClick = { viewModel.setGenreFilter("ALL") },
                                    label = { Text(stringResource(R.string.reading_all_genres)) }
                                )
                                state.availableGenres.forEach { genre ->
                                    FilterChip(
                                        selected = state.selectedGenre == genre,
                                        onClick = { viewModel.setGenreFilter(genre) },
                                        label = { Text(genre) }
                                    )
                                }
                            }
                        }
                    }

                    items(state.passages, key = { it.passage.id }) { item ->
                        val p = item.passage
                        ContextaCard(
                            onClick = { onOpenPassage(p.id) },
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .testTag("reading_passage_${p.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPill(label = p.level)
                                    StatusPill(label = p.genre)
                                }
                                item.bestAccuracyPercent?.let { acc ->
                                    StatusPill(
                                        label = stringResource(R.string.grammar_accuracy_pct, acc),
                                        isPositive = acc >= 75
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(text = p.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = p.paragraphs.firstOrNull()?.text.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
