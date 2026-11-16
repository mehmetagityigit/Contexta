package com.example.feature.listening

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.common.ListeningPlayerController
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.ContextaLoadingView
import com.example.core.designsystem.StatusPill
import com.example.core.model.AppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListeningSessionScreen(
    lessonId: String,
    viewModel: ListeningViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listUiState.collectAsStateWithLifecycle()
    val item = state.lessons.firstOrNull { it.lesson.id == lessonId }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val playerController = remember(context) { ListeningPlayerController(context, scope) }
    val playbackState by playerController.state.collectAsStateWithLifecycle()

    var showTranscript by remember(lessonId) { mutableStateOf(false) }
    val selectedAnswers = remember(lessonId) { mutableStateMapOf<String, String>() }
    var submitted by remember(lessonId) { mutableStateOf(false) }
    val startedAt by remember(lessonId) { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingCaptureSentence by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item?.lesson?.id) {
        val lesson = item?.lesson ?: return@LaunchedEffect
        viewModel.saveLastRoute(lesson.id)
        playerController.prepare(
            audioMode = lesson.audioMode,
            audioUri = lesson.audioUri,
            transcript = lesson.transcript
        )
    }

    DisposableEffect(playerController) {
        onDispose { playerController.release() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(item?.lesson?.title ?: stringResource(R.string.module_listening)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("listening_session_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val lesson = item?.lesson
        if (lesson == null) {
            ContextaLoadingView(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        val isEn = state.language == AppLanguage.ENGLISH

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Audio Controller Card
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            label = if (playbackState.isTtsMode) {
                                stringResource(R.string.listening_mode_tts_label)
                            } else {
                                stringResource(R.string.listening_mode_media3_label)
                            },
                            isWarning = playbackState.isTtsMode,
                            isPositive = !playbackState.isTtsMode
                        )
                        StatusPill(label = lesson.level)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(text = lesson.title, style = MaterialTheme.typography.titleLarge)

                    if (playbackState.isTtsMode) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.listening_tts_line_progress,
                                (playbackState.activeLineIndex + 1).coerceAtMost(playbackState.totalLines.coerceAtLeast(1)),
                                playbackState.totalLines
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.listening_tts_seek_notice),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        val dur = playbackState.durationMs.coerceAtLeast(1L)
                        Slider(
                            value = playbackState.currentPositionMs.toFloat().coerceIn(0f, dur.toFloat()),
                            onValueChange = { playerController.seekToMillis(it.toLong()) },
                            valueRange = 0f..dur.toFloat(),
                            enabled = playbackState.isReady
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playbackState.isTtsMode) {
                            IconButton(
                                onClick = { playerController.stepTtsLine(-1) },
                                modifier = Modifier.size(48.dp).testTag("listening_prev_line")
                            ) {
                                Icon(Icons.Outlined.SkipPrevious, contentDescription = stringResource(R.string.listening_prev_line_cd))
                            }
                        } else {
                            IconButton(
                                onClick = { playerController.seekByMillis(-10_000L) },
                                modifier = Modifier.size(48.dp).testTag("listening_replay_10")
                            ) {
                                Icon(Icons.Outlined.Replay10, contentDescription = stringResource(R.string.listening_replay_10_cd))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { playerController.togglePlayPause() },
                            enabled = playbackState.isReady,
                            modifier = Modifier.height(52.dp).testTag("listening_play_pause")
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = stringResource(R.string.listening_play_pause_cd)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (playbackState.isPlaying) stringResource(R.string.listening_pause)
                                else stringResource(R.string.listening_play)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        if (playbackState.isTtsMode) {
                            IconButton(
                                onClick = { playerController.stepTtsLine(+1) },
                                modifier = Modifier.size(48.dp).testTag("listening_next_line")
                            ) {
                                Icon(Icons.Outlined.SkipNext, contentDescription = stringResource(R.string.listening_next_line_cd))
                            }
                        } else {
                            IconButton(
                                onClick = { playerController.seekByMillis(10_000L) },
                                modifier = Modifier.size(48.dp).testTag("listening_forward_10")
                            ) {
                                Icon(Icons.Outlined.Forward10, contentDescription = stringResource(R.string.listening_forward_10_cd))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                            FilterChip(
                                selected = playbackState.playbackSpeed == spd,
                                onClick = { playerController.setPlaybackSpeed(spd) },
                                label = { Text("${spd}x") }
                            )
                        }
                    }

                    if (playbackState.errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = playbackState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showTranscript = !showTranscript },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("listening_transcript_toggle")
                    ) {
                        Text(
                            if (showTranscript) stringResource(R.string.listening_hide_transcript)
                            else stringResource(R.string.listening_show_transcript)
                        )
                    }
                }
            }

            if (showTranscript) {
                itemsIndexed(lesson.transcript, key = { _, line -> line.id }) { idx, line ->
                    val isCurrent = idx == playbackState.activeLineIndex && playbackState.isPlaying
                    ContextaCard(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .then(
                                if (isCurrent) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                } else Modifier
                            )
                    ) {
                        if (line.speaker.isNotBlank()) {
                            Text(
                                text = line.speaker,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(text = line.text, style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { pendingCaptureSentence = line.text }) {
                                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.reading_save_from_paragraph))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.listening_questions_header, lesson.questions.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                )
            }

            itemsIndexed(lesson.questions, key = { _, q -> q.id }) { idx, q ->
                val chosen = selectedAnswers[q.id]
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(text = "${idx + 1}. ${q.prompt}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    q.options.forEach { opt ->
                        val isSelected = chosen == opt.id
                        val isCorrect = opt.id == q.correctOptionId
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { if (!submitted) selectedAnswers[q.id] = opt.id },
                                modifier = Modifier.testTag("listening_opt_${q.id}_${opt.id}")
                            )
                            Text(
                                text = opt.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    submitted && isCorrect -> MaterialTheme.colorScheme.secondary
                                    submitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (submitted) {
                        Spacer(Modifier.height(6.dp))
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
                    }
                }
            }

            item {
                Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    if (!submitted) {
                        Button(
                            onClick = {
                                submitted = true
                                scope.launch {
                                    viewModel.recordCompletion(lesson, selectedAnswers, startedAt)
                                }
                            },
                            enabled = selectedAnswers.size == lesson.questions.size,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("listening_submit_button")
                        ) {
                            Text(stringResource(R.string.practice_check_answers))
                        }
                    } else {
                        val correct = lesson.questions.count { selectedAnswers[it.id] == it.correctOptionId }
                        ContextaCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.practice_score_result, correct, lesson.questions.size),
                                style = MaterialTheme.typography.titleMedium
                            )
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

    val captureSentence = pendingCaptureSentence
    if (captureSentence != null && item != null) {
        var word by remember(captureSentence) { mutableStateOf("") }
        var meaningTr by remember(captureSentence) { mutableStateOf("") }
        var sentence by remember(captureSentence) { mutableStateOf(captureSentence) }
        var saving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!saving) pendingCaptureSentence = null },
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
                            val res = viewModel.saveWordFromTranscript(
                                expression = word,
                                meaningTr = meaningTr,
                                sentence = sentence,
                                lessonTitle = item.lesson.title
                            )
                            saving = false
                            pendingCaptureSentence = null
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("Saved: $word")
                            }
                        }
                    },
                    enabled = !saving && word.isNotBlank()
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCaptureSentence = null }, enabled = !saving) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
