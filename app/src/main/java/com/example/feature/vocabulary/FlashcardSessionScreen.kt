package com.example.feature.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.ContextaEmptyView
import com.example.core.designsystem.StatusPill
import com.example.core.model.ReviewRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardSessionScreen(
    viewModel: VocabularyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.flashcardState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!state.isActive) {
            viewModel.startFlashcardSession(includeAllIfNoneDue = false)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.flashcard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("flashcard_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val containerModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()

            if (state.isCompleted && state.queue.isEmpty()) {
                ContextaEmptyView(
                    icon = Icons.Outlined.CheckCircle,
                    title = stringResource(R.string.flashcard_all_caught_up_title),
                    subtitle = stringResource(R.string.flashcard_all_caught_up_subtitle),
                    actionLabel = stringResource(R.string.flashcard_practice_anyway),
                    onAction = { viewModel.startFlashcardSession(includeAllIfNoneDue = true) },
                    modifier = containerModifier
                )
                return@Scaffold
            }

            if (state.isCompleted) {
                ContextaCard(modifier = containerModifier) {
                    Text(
                        text = stringResource(R.string.flashcard_summary_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.flashcard_summary_reviewed, state.reviewedCount),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.flashcard_summary_breakdown,
                            state.recalledCount,
                            state.againCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("flashcard_done_button")
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
                return@Scaffold
            }

            val current = state.queue.getOrNull(state.currentIndex) ?: return@Scaffold
            val total = state.queue.size.coerceAtLeast(1)
            val progress = (state.currentIndex.toFloat() / total.toFloat()).coerceIn(0f, 1f)

            Row(
                modifier = containerModifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.flashcard_progress, state.currentIndex + 1, total),
                    style = MaterialTheme.typography.labelLarge
                )
                if (current.entry.partOfSpeech.isNotBlank()) {
                    StatusPill(label = current.entry.partOfSpeech)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = containerModifier.height(8.dp)
            )
            Spacer(Modifier.height(20.dp))

            // Active Recall Card
            ContextaCard(modifier = containerModifier) {
                Text(
                    text = current.entry.expression,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                val firstCtx = current.contexts.firstOrNull()
                if (firstCtx != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "\"${firstCtx.sentence}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (firstCtx.sourceTitle.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            StatusPill(label = firstCtx.sourceTitle)
                        }
                    }
                }

                if (state.isAnswerRevealed) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = current.entry.meaningTr.ifBlank { "-" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (current.entry.definitionEn.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = current.entry.definitionEn,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (current.entry.personalNote.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = current.entry.personalNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (state.errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(20.dp))

            if (!state.isAnswerRevealed) {
                Button(
                    onClick = { viewModel.revealAnswer() },
                    modifier = containerModifier.height(52.dp).testTag("flashcard_reveal_button")
                ) {
                    Text(stringResource(R.string.flashcard_show_answer))
                }
            } else {
                Row(
                    modifier = containerModifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.rateCurrentCard(ReviewRating.AGAIN) },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f).height(52.dp).testTag("flashcard_rate_again")
                    ) {
                        Text(stringResource(R.string.flashcard_rate_again))
                    }
                    OutlinedButton(
                        onClick = { viewModel.rateCurrentCard(ReviewRating.HARD) },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f).height(52.dp).testTag("flashcard_rate_hard")
                    ) {
                        Text(stringResource(R.string.flashcard_rate_hard))
                    }
                    Button(
                        onClick = { viewModel.rateCurrentCard(ReviewRating.GOOD) },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f).height(52.dp).testTag("flashcard_rate_good")
                    ) {
                        Text(stringResource(R.string.flashcard_rate_good))
                    }
                    Button(
                        onClick = { viewModel.rateCurrentCard(ReviewRating.EASY) },
                        enabled = !state.isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f).height(52.dp).testTag("flashcard_rate_easy")
                    ) {
                        Text(stringResource(R.string.flashcard_rate_easy))
                    }
                }
            }
        }
    }
}
