package com.example.feature.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.DemoModeBanner
import com.example.core.designsystem.StatusPill
import com.example.core.model.AppLanguage
import com.example.core.model.CefrLevel
import com.example.core.model.TargetExam
import com.example.core.model.ThemeMode
import com.example.feature.auth.examLabel
import com.example.feature.overlay.OverlayPermissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignedOut: () -> Unit,
    onSwitchToRealAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusMessage()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.buildExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_success))
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(err.localizedMessage ?: "Export failed")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!text.isNullOrBlank()) {
                    viewModel.importJson(text)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isDemoMode) {
                item { DemoModeBanner(modifier = Modifier.widthIn(max = 600.dp)) }
            }

            item {
                Text(
                    text = stringResource(R.string.nav_settings),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                )
            }

            // Account Card
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_account_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.isDemoMode) {
                        StatusPill(label = stringResource(R.string.sync_badge_demo), isWarning = true)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onSwitchToRealAuth,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_switch_to_auth")
                        ) {
                            Text(stringResource(R.string.auth_google_sign_in))
                        }
                    } else {
                        Text(
                            text = state.userDisplayName ?: stringResource(R.string.settings_signed_in_user),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!state.userEmail.isNullOrBlank()) {
                            Text(
                                text = state.userEmail.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.signOut(context, onSignedOut) },
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_sign_out_button")
                        ) {
                            Text(stringResource(R.string.settings_sign_out))
                        }
                    }
                }
            }

            // Language & Theme Card
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_language_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.settings.language == AppLanguage.TURKISH,
                            onClick = {
                                viewModel.updateLanguage(context, AppLanguage.TURKISH) {
                                    (context as? Activity)?.recreate()
                                }
                            },
                            label = { Text(stringResource(R.string.language_turkish)) },
                            modifier = Modifier.testTag("settings_lang_tr")
                        )
                        FilterChip(
                            selected = state.settings.language == AppLanguage.ENGLISH,
                            onClick = {
                                viewModel.updateLanguage(context, AppLanguage.ENGLISH) {
                                    (context as? Activity)?.recreate()
                                }
                            },
                            label = { Text(stringResource(R.string.language_english)) },
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            }
                            FilterChip(
                                selected = state.settings.themeMode == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Learning Preferences Card
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.onboarding_goal_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TargetExam.entries.forEach { exam ->
                            FilterChip(
                                selected = state.settings.targetExam == exam,
                                onClick = { viewModel.updateTargetExam(exam) },
                                label = { Text(examLabel(exam)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.onboarding_level_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CefrLevel.entries.forEach { lvl ->
                            FilterChip(
                                selected = state.settings.cefrLevel == lvl,
                                onClick = { viewModel.updateCefrLevel(lvl) },
                                label = { Text(lvl.code) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.onboarding_daily_goal_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 30, 45).forEach { mins ->
                            FilterChip(
                                selected = state.settings.dailyGoalMinutes == mins,
                                onClick = { viewModel.updateDailyGoal(mins) },
                                label = { Text(stringResource(R.string.minutes_format, mins)) }
                            )
                        }
                    }
                }
            }

            // Floating Overlay Settings Card
            item {
                val hasPerm = OverlayPermissionHelper.canDrawOverlays(context)
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_overlay_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (hasPerm) stringResource(R.string.home_overlay_ready)
                        else stringResource(R.string.home_overlay_perm_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(
                            R.string.settings_overlay_scale,
                            (state.settings.overlayScale * 100).toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = state.settings.overlayScale,
                        onValueChange = { viewModel.updateOverlayVisuals(it, state.settings.overlayAlpha) },
                        valueRange = 0.75f..1.35f
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_overlay_alpha,
                            (state.settings.overlayAlpha * 100).toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = state.settings.overlayAlpha,
                        onValueChange = { viewModel.updateOverlayVisuals(state.settings.overlayScale, it) },
                        valueRange = 0.5f..1.0f
                    )
                    if (!hasPerm) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(OverlayPermissionHelper.buildOverlayPermissionIntent(context))
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(stringResource(R.string.home_overlay_grant_perm))
                        }
                    }
                }
            }

            // Export / Import & Data Deletion Card
            item {
                ContextaCard(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_data_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("contexta_backup.json") },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("settings_export_json")
                        ) {
                            Text(stringResource(R.string.settings_export_json))
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("settings_import_json")
                        ) {
                            Text(stringResource(R.string.settings_import_json))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showDeleteConfirm = true },
                        enabled = !state.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_delete_account")
                    ) {
                        Text(stringResource(R.string.settings_delete_all_data))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAllDataAndAccount(context, onSignedOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
