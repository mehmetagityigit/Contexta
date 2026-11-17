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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.ContextaEmptyView
import com.example.core.designsystem.ContextaLoadingView
import com.example.core.designsystem.ContextaNavyCard
import com.example.core.designsystem.DemoModeBanner
import com.example.core.designsystem.LocalContextaColors
import com.example.core.designsystem.StatusPill
import com.example.core.designsystem.SyncBadge
import com.example.core.model.ContextSourceType
import com.example.core.model.VocabularyFilter
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabularyListScreen(
    viewModel: VocabularyViewModel,
    onOpenDetail: (String) -> Unit,
    onStartFlashcards: () -> Unit,
    onOpenVoiceCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listUiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    if (state.isLoading) {
        ContextaLoadingView(modifier = modifier)
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("vocab_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.vocab_add_word)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isDemoMode) {
                item { DemoModeBanner(modifier = Modifier.widthIn(max = 600.dp)) }
            }

            // Flashcard review CTA & Voice add row (Featured Dark Navy banner)
            item {
                val navy = LocalContextaColors.current
                ContextaNavyCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vocab_srs_banner_title, state.dueCount),
                                style = MaterialTheme.typography.titleMedium,
                                color = navy.onNavySurface
                            )
                            Text(
                                text = stringResource(R.string.vocab_total_count_label, state.totalCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = navy.onNavyMuted
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onOpenVoiceCapture,
                                modifier = Modifier.size(48.dp).testTag("vocab_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = stringResource(R.string.voice_capture_title),
                                    tint = navy.onNavySurface
                                )
                            }
                            Button(
                                onClick = onStartFlashcards,
                                enabled = state.totalCount > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = navy.navyActionBg,
                                    contentColor = navy.navyActionText,
                                    disabledContainerColor = navy.navyPillBg,
                                    disabledContentColor = navy.onNavyMuted
                                ),
                                modifier = Modifier.height(48.dp).testTag("vocab_start_srs_button")
                            ) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.home_start_review))
                            }
                        }
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.vocab_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.action_clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().testTag("vocab_search_input")
                )
            }

            // Filters
            item {
                FlowRow(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VocabularyFilter.entries.forEach { filter ->
                        val label = when (filter) {
                            VocabularyFilter.ALL -> stringResource(R.string.vocab_filter_all)
                            VocabularyFilter.DUE_TODAY -> stringResource(R.string.vocab_filter_due)
                            VocabularyFilter.NEW -> stringResource(R.string.vocab_filter_new)
                            VocabularyFilter.FAVORITES -> stringResource(R.string.vocab_filter_favorites)
                        }
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = { viewModel.onFilterChange(filter) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("vocab_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (state.items.isEmpty()) {
                item {
                    ContextaEmptyView(
                        icon = Icons.Outlined.AutoStories,
                        title = stringResource(R.string.vocab_empty_title),
                        subtitle = stringResource(R.string.vocab_empty_subtitle),
                        actionLabel = stringResource(R.string.vocab_add_word),
                        onAction = { showAddDialog = true },
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
            } else {
                items(state.items, key = { it.entry.id }) { item ->
                    ContextaCard(
                        onClick = { onOpenDetail(item.entry.id) },
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .testTag("vocab_card_${item.entry.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.entry.expression,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (item.entry.meaningTr.isNotBlank()) {
                                    Text(
                                        text = item.entry.meaningTr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SyncBadge(syncStatus = item.entry.syncStatus, isOnline = state.isOnline)
                                IconButton(
                                    onClick = {
                                        viewModel.toggleFavorite(item.entry.id, !item.entry.favorite)
                                    },
                                    modifier = Modifier.size(48.dp).testTag("vocab_fav_${item.entry.id}")
                                ) {
                                    Icon(
                                        imageVector = if (item.entry.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = stringResource(R.string.vocab_favorite_cd),
                                        tint = if (item.entry.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        val firstCtx = item.contexts.firstOrNull()
                        if (firstCtx != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"${firstCtx.sentence}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (firstCtx.sourceTitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusPill(label = firstCtx.sourceTitle)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVocabularyDialog(
            onDismiss = { showAddDialog = false },
            onSave = { expr, tr, en, pos, note, tags, ctx, src, srcType ->
                viewModel.saveNewWord(expr, tr, en, pos, note, tags, ctx, src, srcType)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddVocabularyDialog(
    onDismiss: () -> Unit,
    onSave: suspend (
        expression: String,
        meaningTr: String,
        definitionEn: String,
        partOfSpeech: String,
        personalNote: String,
        tagsRaw: String,
        contextSentence: String,
        sourceTitle: String,
        sourceType: ContextSourceType
    ) -> Result<String>
) {
    val scope = rememberCoroutineScope()
    var expression by remember { mutableStateOf("") }
    var meaningTr by remember { mutableStateOf("") }
    var definitionEn by remember { mutableStateOf("") }
    var partOfSpeech by remember { mutableStateOf("") }
    var personalNote by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var contextSentence by remember { mutableStateOf("") }
    var sourceTitle by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(ContextSourceType.MANUAL) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.vocab_add_word)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = expression,
                        onValueChange = { expression = it },
                        label = { Text(stringResource(R.string.vocab_field_expression)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_vocab_expression")
                    )
                }
                item {
                    OutlinedTextField(
                        value = meaningTr,
                        onValueChange = { meaningTr = it },
                        label = { Text(stringResource(R.string.vocab_field_meaning_tr)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_vocab_meaning_tr")
                    )
                }
                item {
                    OutlinedTextField(
                        value = definitionEn,
                        onValueChange = { definitionEn = it },
                        label = { Text(stringResource(R.string.vocab_field_definition_en)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = partOfSpeech,
                            onValueChange = { partOfSpeech = it },
                            label = { Text(stringResource(R.string.vocab_field_pos)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tagsRaw,
                            onValueChange = { tagsRaw = it },
                            label = { Text(stringResource(R.string.vocab_field_tags)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = contextSentence,
                        onValueChange = { contextSentence = it },
                        label = { Text(stringResource(R.string.vocab_field_context_sentence)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("add_vocab_context")
                    )
                }
                item {
                    OutlinedTextField(
                        value = sourceTitle,
                        onValueChange = { sourceTitle = it },
                        label = { Text(stringResource(R.string.vocab_field_source_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            ContextSourceType.MANUAL,
                            ContextSourceType.VIDEO,
                            ContextSourceType.ARTICLE,
                            ContextSourceType.BOOK,
                            ContextSourceType.PODCAST
                        ).forEach { type ->
                            FilterChip(
                                selected = sourceType == type,
                                onClick = { sourceType = type },
                                label = { Text(type.code) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = personalNote,
                        onValueChange = { personalNote = it },
                        label = { Text(stringResource(R.string.vocab_field_note)) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (errorMsg != null) {
                    item {
                        Text(
                            text = errorMsg.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (expression.trim().isEmpty()) {
                        errorMsg = "Expression required"
                        return@Button
                    }
                    isSaving = true
                    errorMsg = null
                    scope.launch {
                        val res = onSave(
                            expression,
                            meaningTr,
                            definitionEn,
                            partOfSpeech,
                            personalNote,
                            tagsRaw,
                            contextSentence,
                            sourceTitle,
                            sourceType
                        )
                        isSaving = false
                        res.onSuccess { onDismiss() }
                            .onFailure { errorMsg = it.localizedMessage ?: "Failed to save" }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("add_vocab_save_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
