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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.ContextaLoadingView
import com.example.core.designsystem.StatusPill
import com.example.core.model.AppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GrammarDetailScreen(
    topicId: String,
    viewModel: GrammarViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState by viewModel.listUiState.collectAsStateWithLifecycle()
    val topicItem = listState.topics.firstOrNull { it.topic.id == topicId }
    val scope = rememberCoroutineScope()

    val selectedAnswers = remember(topicId) { mutableStateMapOf<String, String>() }
    var submitted by remember(topicId) { mutableStateOf(false) }
    var isSaving by remember(topicId) { mutableStateOf(false) }
    var saveFeedback by remember(topicId) { mutableStateOf<String?>(null) }
    val startedAt by remember(topicId) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(topicId) {
        viewModel.saveLastRoute(topicId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(topicItem?.topic?.title ?: stringResource(R.string.module_grammar)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("grammar_detail_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val item = topicItem
        if (item == null) {
            ContextaLoadingView(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        val topic = item.topic
        val isEn = listState.language == AppLanguage.ENGLISH

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Summary Card
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(label = topic.level)
                        topic.targetExams.forEach { StatusPill(label = it) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(text = topic.title, style = MaterialTheme.typography.headlineSmall)
                    if (topic.titleTr.isNotBlank()) {
                        Text(
                            text = topic.titleTr,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (isEn && topic.summaryEn.isNotBlank()) topic.summaryEn else topic.summaryTr,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Examples Card
            if (topic.examples.isNotEmpty()) {
                item {
                    ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.grammar_examples_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(10.dp))
                        topic.examples.forEachIndexed { idx, ex ->
                            if (idx > 0) Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = ex.correct, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = ex.incorrect,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isEn && ex.noteEn.isNotBlank()) ex.noteEn else ex.noteTr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Common Mistakes Card
            val mistakes = if (isEn && topic.commonMistakesEn.isNotEmpty()) {
                topic.commonMistakesEn
            } else {
                topic.commonMistakesTr
            }
            if (mistakes.isNotEmpty()) {
                item {
                    ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.grammar_mistakes_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        mistakes.forEach { m ->
                            Text(
                                text = "• $m",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Practice Questions Header
            item {
                Text(
                    text = stringResource(R.string.grammar_quiz_title, topic.questions.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                )
            }

            itemsIndexed(topic.questions, key = { _, q -> q.id }) { qIdx, q ->
                val chosenId = selectedAnswers[q.id]
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = "${qIdx + 1}. ${q.prompt}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    q.options.forEach { opt ->
                        val isSelected = chosenId == opt.id
                        val isCorrectOption = opt.id == q.correctOptionId
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (!submitted) selectedAnswers[q.id] = opt.id
                                },
                                modifier = Modifier.testTag("grammar_opt_${q.id}_${opt.id}")
                            )
                            Text(
                                text = opt.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    submitted && isCorrectOption -> MaterialTheme.colorScheme.secondary
                                    submitted && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (submitted) {
                        Spacer(Modifier.height(8.dp))
                        val chosenOption = q.options.firstOrNull { it.id == chosenId }
                        val specificNote = if (isEn) chosenOption?.explanationEn else chosenOption?.explanationTr
                        val generalNote = if (isEn && q.explanationEn.isNotBlank()) q.explanationEn else q.explanationTr
                        val isRight = chosenId == q.correctOptionId
                        Text(
                            text = if (isRight) {
                                stringResource(R.string.practice_correct_label)
                            } else {
                                stringResource(R.string.practice_wrong_label)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isRight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                        if (!isRight && !specificNote.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = specificNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = generalNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Submit / Result Footer
            item {
                Column(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!submitted) {
                        Button(
                            onClick = {
                                submitted = true
                                isSaving = true
                                scope.launch {
                                    val res = viewModel.recordCompletion(topic, selectedAnswers, startedAt)
                                    isSaving = false
                                    saveFeedback = if (res.isSuccess) {
                                        null
                                    } else {
                                        res.exceptionOrNull()?.localizedMessage
                                    }
                                }
                            },
                            enabled = selectedAnswers.size == topic.questions.size && !isSaving,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("grammar_submit_button")
                        ) {
                            Text(stringResource(R.string.practice_check_answers))
                        }
                    } else {
                        val correctCount = topic.questions.count { selectedAnswers[it.id] == it.correctOptionId }
                        ContextaCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(
                                    R.string.practice_score_result,
                                    correctCount,
                                    topic.questions.size
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (saveFeedback != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = saveFeedback.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    selectedAnswers.clear()
                                    submitted = false
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(stringResource(R.string.practice_retry_quiz))
                            }
                        }
                    }
                }
            }
        }
    }
}
