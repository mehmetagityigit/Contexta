package com.example.feature.vocabulary

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.common.VoiceCaptureController
import com.example.core.common.VoiceCaptureState
import com.example.core.common.VoiceErrorType
import com.example.core.designsystem.ContextaCard
import com.example.core.model.ContextSourceType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceQuickCaptureScreen(
    initialExpression: String,
    viewModel: VocabularyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { VoiceCaptureController(context) }
    val voiceState by controller.state.collectAsStateWithLifecycle()

    var expression by remember { mutableStateOf(initialExpression) }
    var meaningTr by remember { mutableStateOf("") }
    var contextSentence by remember { mutableStateOf("") }
    var sourceTitle by remember { mutableStateOf("") }
    var permissionDeniedMsg by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    LaunchedEffect(voiceState) {
        when (val st = voiceState) {
            is VoiceCaptureState.Partial -> expression = st.text
            is VoiceCaptureState.Recognized -> expression = st.text
            else -> Unit
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedMsg = false
            controller.startListening()
        } else {
            permissionDeniedMsg = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_capture_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("voice_back_button")) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContextaCard(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.voice_capture_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                val isListening = voiceState is VoiceCaptureState.Listening
                FilledTonalButton(
                    onClick = {
                        if (isListening) {
                            controller.stopListening()
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                controller.startListening()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("voice_ptt_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Outlined.Stop else Icons.Outlined.Mic,
                        contentDescription = stringResource(R.string.voice_mic_cd),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isListening) {
                            stringResource(R.string.voice_listening_stop)
                        } else {
                            stringResource(R.string.voice_tap_to_speak)
                        }
                    )
                }

                if (permissionDeniedMsg) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.voice_permission_denied),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                val st = voiceState
                if (st is VoiceCaptureState.Error) {
                    Spacer(Modifier.height(8.dp))
                    val errText = when (st.errorType) {
                        VoiceErrorType.SERVICE_UNAVAILABLE -> stringResource(R.string.voice_error_unavailable)
                        VoiceErrorType.PERMISSION_DENIED -> stringResource(R.string.voice_permission_denied)
                        VoiceErrorType.NO_MATCH -> stringResource(R.string.voice_error_no_match)
                        else -> stringResource(R.string.voice_error_try_again)
                    }
                    Text(
                        text = errText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            ContextaCard(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it },
                    label = { Text(stringResource(R.string.vocab_field_expression)) },
                    modifier = Modifier.fillMaxWidth().testTag("voice_expression_input")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = meaningTr,
                    onValueChange = { meaningTr = it },
                    label = { Text(stringResource(R.string.vocab_field_meaning_tr)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = contextSentence,
                    onValueChange = { contextSentence = it },
                    label = { Text(stringResource(R.string.vocab_field_context_sentence)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = sourceTitle,
                    onValueChange = { sourceTitle = it },
                    label = { Text(stringResource(R.string.vocab_field_source_title)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (feedbackMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = feedbackMsg.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (expression.trim().isEmpty()) return@Button
                        isSaving = true
                        feedbackMsg = null
                        scope.launch {
                            val res = viewModel.saveNewWord(
                                expression = expression,
                                meaningTr = meaningTr,
                                definitionEn = "",
                                partOfSpeech = "",
                                personalNote = "",
                                tagsRaw = "",
                                contextSentence = contextSentence,
                                sourceTitle = sourceTitle,
                                sourceType = ContextSourceType.VOICE
                            )
                            isSaving = false
                            res.onSuccess {
                                onBack()
                            }.onFailure { err ->
                                feedbackMsg = err.localizedMessage ?: "Failed to save"
                            }
                        }
                    },
                    enabled = !isSaving && expression.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("voice_save_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
