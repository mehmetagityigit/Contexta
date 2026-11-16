package com.example.feature.vocabulary

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.designsystem.ContextaEmptyView
import com.example.core.designsystem.StatusPill
import com.example.core.model.ContextSourceType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VocabularyDetailScreen(
    vocabId: String,
    viewModel: VocabularyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allItems by viewModel.observeAllCombined().collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val item = allItems.firstOrNull { it.entry.id == vocabId }

    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddContextDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(item?.entry?.expression ?: stringResource(R.string.vocab_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("vocab_detail_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(item.entry.id, !item.entry.favorite) },
                            modifier = Modifier.testTag("vocab_detail_favorite")
                        ) {
                            Icon(
                                imageVector = if (item.entry.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.vocab_favorite_cd)
                            )
                        }
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("vocab_detail_edit")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("vocab_detail_delete")
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (item == null) {
            ContextaEmptyView(
                icon = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.vocab_not_found_title),
                subtitle = stringResource(R.string.vocab_not_found_subtitle),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Core meaning card
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = item.entry.expression,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (item.entry.partOfSpeech.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        StatusPill(label = item.entry.partOfSpeech)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.vocab_field_meaning_tr),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.entry.meaningTr.ifBlank { "-" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (item.entry.definitionEn.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.vocab_field_definition_en),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.entry.definitionEn,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (item.entry.personalNote.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.vocab_field_note),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.entry.personalNote,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (item.entry.tags.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.entry.tags.forEach { tag -> StatusPill(label = tag) }
                        }
                    }
                }
            }

            // SRS info card
            item {
                val rs = item.reviewState
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.vocab_srs_stats_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.vocab_srs_stats_body,
                            rs?.intervalDays ?: 0,
                            rs?.repetitionCount ?: 0,
                            rs?.easeFactor ?: 2.5
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Optional on-demand AI helper card
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.vocab_ai_helper_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.requestAiInsight(
                                    item.entry.expression,
                                    item.contexts.firstOrNull()?.sentence
                                )
                            },
                            enabled = !aiState.isLoading,
                            modifier = Modifier.height(48.dp).testTag("vocab_ai_insight_button")
                        ) {
                            if (aiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.vocab_ai_helper_cta))
                            }
                        }
                    }
                    val insight = aiState.insight
                    if (insight != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(text = "${insight.meaningTr} (${insight.partOfSpeech})", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(text = insight.definitionEn, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(text = "\"${insight.exampleSentence}\"", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = insight.usageNoteTr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.updateWord(
                                        item.entry.copy(
                                            meaningTr = item.entry.meaningTr.ifBlank { insight.meaningTr },
                                            definitionEn = item.entry.definitionEn.ifBlank { insight.definitionEn },
                                            partOfSpeech = item.entry.partOfSpeech.ifBlank { insight.partOfSpeech }
                                        )
                                    )
                                }
                            }
                        ) {
                            Text(stringResource(R.string.vocab_ai_apply_meaning))
                        }
                    } else if (aiState.errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = aiState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Contexts Header
            item {
                Row(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.vocab_contexts_header, item.contexts.size),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedButton(
                        onClick = { showAddContextDialog = true },
                        modifier = Modifier.height(48.dp).testTag("vocab_add_context_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.vocab_add_context))
                    }
                }
            }

            items(item.contexts, key = { it.id }) { ctx ->
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = "\"${ctx.sentence}\"",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(label = ctx.sourceType.code)
                        if (ctx.sourceTitle.isNotBlank()) {
                            StatusPill(label = ctx.sourceTitle)
                        }
                    }
                }
            }
        }
    }

    if (showAddContextDialog && item != null) {
        var sentence by remember { mutableStateOf("") }
        var sourceTitle by remember { mutableStateOf("") }
        var saving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!saving) showAddContextDialog = false },
            title = { Text(stringResource(R.string.vocab_add_context)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sentence,
                        onValueChange = { sentence = it },
                        label = { Text(stringResource(R.string.vocab_field_context_sentence)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sourceTitle,
                        onValueChange = { sourceTitle = it },
                        label = { Text(stringResource(R.string.vocab_field_source_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sentence.isBlank()) return@Button
                        saving = true
                        scope.launch {
                            viewModel.addContext(item.entry.id, sentence, sourceTitle, ContextSourceType.MANUAL)
                            saving = false
                            showAddContextDialog = false
                        }
                    },
                    enabled = !saving && sentence.isNotBlank()
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddContextDialog = false }, enabled = !saving) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showEditDialog && item != null) {
        var meaningTr by remember { mutableStateOf(item.entry.meaningTr) }
        var definitionEn by remember { mutableStateOf(item.entry.definitionEn) }
        var pos by remember { mutableStateOf(item.entry.partOfSpeech) }
        var note by remember { mutableStateOf(item.entry.personalNote) }
        var saving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!saving) showEditDialog = false },
            title = { Text(stringResource(R.string.action_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = meaningTr,
                        onValueChange = { meaningTr = it },
                        label = { Text(stringResource(R.string.vocab_field_meaning_tr)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = definitionEn,
                        onValueChange = { definitionEn = it },
                        label = { Text(stringResource(R.string.vocab_field_definition_en)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pos,
                        onValueChange = { pos = it },
                        label = { Text(stringResource(R.string.vocab_field_pos)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.vocab_field_note)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saving = true
                        scope.launch {
                            viewModel.updateWord(
                                item.entry.copy(
                                    meaningTr = meaningTr.trim(),
                                    definitionEn = definitionEn.trim(),
                                    partOfSpeech = pos.trim(),
                                    personalNote = note.trim()
                                )
                            )
                            saving = false
                            showEditDialog = false
                        }
                    },
                    enabled = !saving
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }, enabled = !saving) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteConfirm && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.vocab_delete_confirm_title)) },
            text = { Text(stringResource(R.string.vocab_delete_confirm_body, item.entry.expression)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.deleteWord(item.entry.id)
                            showDeleteConfirm = false
                            onBack()
                        }
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
