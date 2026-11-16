package com.example.feature.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.ContextaLoadingView
import com.example.core.designsystem.StatusPill
import com.example.core.model.AppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReadingSessionScreen(
    passageId: String,
    viewModel: ReadingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listUiState.collectAsStateWithLifecycle()
    val item = state.passages.firstOrNull { it.passage.id == passageId }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedAnswers = remember(passageId) { mutableStateMapOf<String, String>() }
    var submitted by remember(passageId) { mutableStateOf(false) }
    var highlightedEvidenceId by remember(passageId) { mutableStateOf<String?>(null) }
    val startedAt by remember(passageId) { mutableLongStateOf(System.currentTimeMillis()) }

    var pendingCaptureWord by remember { mutableStateOf<Pair<String, String>?>(null) } // word to sentence

    LaunchedEffect(passageId) {
        viewModel.saveLastRoute(passageId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(item?.passage?.title ?: stringResource(R.string.module_reading)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reading_session_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.adjustFontSize(-2) },
                        modifier = Modifier.testTag("reading_font_smaller")
                    ) {
                        Text("A-", style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(
                        onClick = { viewModel.adjustFontSize(+2) },
                        modifier = Modifier.testTag("reading_font_larger")
                    ) {
                        Icon(Icons.Outlined.FormatSize, contentDescription = stringResource(R.string.reading_font_size_cd))
                    }
                }
            )
        }
    ) { innerPadding ->
        val passage = item?.passage
        if (passage == null) {
            ContextaLoadingView(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        val isEn = state.language == AppLanguage.ENGLISH
        val lineHeightSp = (state.fontSizeSp * 1.5f).sp

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(label = passage.level)
                        StatusPill(label = passage.genre)
                        passage.targetExams.forEach { StatusPill(label = it) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(text = passage.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.reading_tap_word_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(passage.paragraphs, key = { it.id }) { para ->
                val isEvidence = highlightedEvidenceId == para.id
                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                ContextaCard(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .then(
                            if (isEvidence) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                    shape = MaterialTheme.shapes.medium
                                )
                            } else Modifier
                        )
                ) {
                    if (isEvidence) {
                        StatusPill(
                            label = stringResource(R.string.reading_evidence_highlighted),
                            isPositive = true
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = para.text,
                        fontSize = state.fontSizeSp.sp,
                        lineHeight = lineHeightSp,
                        fontFamily = FontFamily.Serif,
                        onTextLayout = { textLayoutResult = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(para.text) {
                                detectTapGestures { offset ->
                                    val layout = textLayoutResult ?: return@detectTapGestures
                                    val charIndex = layout.getOffsetForPosition(offset)
                                    val extracted = extractWordAndSentence(para.text, charIndex)
                                    if (extracted != null) {
                                        pendingCaptureWord = extracted
                                    }
                                }
                            }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val firstSentence = extractSentenceAt(para.text, 0)
                                pendingCaptureWord = "" to firstSentence
                            },
                            modifier = Modifier.testTag("reading_capture_para_${para.id}")
                        ) {
                            Icon(Icons.Outlined.BookmarkAdd, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.reading_save_from_paragraph))
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.reading_questions_header, passage.questions.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                )
            }

            itemsIndexed(passage.questions, key = { _, q -> q.id }) { idx, q ->
                val chosen = selectedAnswers[q.id]
                ContextaCard(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    if (q.questionType.isNotBlank()) {
                        StatusPill(label = q.questionType)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = "${idx + 1}. ${q.prompt}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    q.options.forEach { opt ->
                        val isSelected = chosen == opt.id
                        val isCorrectOption = opt.id == q.correctOptionId
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { if (!submitted) selectedAnswers[q.id] = opt.id },
                                modifier = Modifier.testTag("reading_opt_${q.id}_${opt.id}")
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
                        val isRight = chosen == q.correctOptionId
                        Text(
                            text = if (isRight) stringResource(R.string.practice_correct_label)
                            else stringResource(R.string.practice_wrong_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isRight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isEn && q.explanationEn.isNotBlank()) q.explanationEn else q.explanationTr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val evidenceId = q.evidenceRefId
                        if (!evidenceId.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    highlightedEvidenceId = if (highlightedEvidenceId == evidenceId) null else evidenceId
                                }
                            ) {
                                Text(stringResource(R.string.reading_show_evidence))
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    if (!submitted) {
                        Button(
                            onClick = {
                                submitted = true
                                scope.launch {
                                    viewModel.recordCompletion(passage, selectedAnswers, startedAt)
                                }
                            },
                            enabled = selectedAnswers.size == passage.questions.size,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("reading_submit_button")
                        ) {
                            Text(stringResource(R.string.practice_check_answers))
                        }
                    } else {
                        val correct = passage.questions.count { selectedAnswers[it.id] == it.correctOptionId }
                        ContextaCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.practice_score_result, correct, passage.questions.size),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    selectedAnswers.clear()
                                    submitted = false
                                    highlightedEvidenceId = null
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

    val capture = pendingCaptureWord
    if (capture != null && item != null) {
        var word by remember(capture) { mutableStateOf(capture.first) }
        var meaningTr by remember(capture) { mutableStateOf("") }
        var sentence by remember(capture) { mutableStateOf(capture.second) }
        var saving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!saving) pendingCaptureWord = null },
            title = { Text(stringResource(R.string.vocab_add_word)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = word,
                        onValueChange = { word = it },
                        label = { Text(stringResource(R.string.vocab_field_expression)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = meaningTr,
                        onValueChange = { meaningTr = it },
                        label = { Text(stringResource(R.string.vocab_field_meaning_tr)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sentence,
                        onValueChange = { sentence = it },
                        label = { Text(stringResource(R.string.vocab_field_context_sentence)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (word.isBlank()) return@Button
                        saving = true
                        scope.launch {
                            val res = viewModel.saveWordFromPassage(
                                expression = word,
                                meaningTr = meaningTr,
                                contextSentence = sentence,
                                passageTitle = item.passage.title
                            )
                            saving = false
                            pendingCaptureWord = null
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("Saved: $word")
                            }
                        }
                    },
                    enabled = !saving && word.isNotBlank()
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCaptureWord = null }, enabled = !saving) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun extractWordAndSentence(text: String, offset: Int): Pair<String, String>? {
    if (text.isEmpty() || offset < 0 || offset >= text.length) return null
    fun isWordChar(c: Char): Boolean = c.isLetter() || c == '\'' || c == '-'
    var idx = offset
    if (!isWordChar(text[idx])) {
        if (idx > 0 && isWordChar(text[idx - 1])) idx -= 1 else return null
    }
    var start = idx
    while (start > 0 && isWordChar(text[start - 1])) start--
    var end = idx
    while (end < text.length - 1 && isWordChar(text[end + 1])) end++
    val word = text.substring(start, end + 1).trim('\'', '-')
    if (word.length < 2) return null
    return word to extractSentenceAt(text, start)
}

private fun extractSentenceAt(text: String, offset: Int): String {
    var start = offset.coerceIn(0, text.length)
    while (start > 0 && text[start - 1] !in charArrayOf('.', '!', '?', '\n')) start--
    var end = offset.coerceIn(0, text.length)
    while (end < text.length && text[end] !in charArrayOf('.', '!', '?', '\n')) end++
    if (end < text.length && text[end] in charArrayOf('.', '!', '?')) end++
    return text.substring(start, end).trim()
}
