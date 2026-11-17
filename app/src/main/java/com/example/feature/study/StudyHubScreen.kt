package com.example.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.ContextaCard
import com.example.core.designsystem.StatusPill

@Composable
fun StudyHubScreen(
    onOpenGrammar: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            Text(
                text = stringResource(R.string.study_hub_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.study_hub_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StudyModuleOptionCard(
            title = stringResource(R.string.module_grammar),
            subtitle = stringResource(R.string.study_grammar_desc),
            badge = stringResource(R.string.study_grammar_badge),
            icon = Icons.Outlined.Spellcheck,
            onClick = onOpenGrammar,
            testTag = "study_hub_grammar_card"
        )

        StudyModuleOptionCard(
            title = stringResource(R.string.module_reading),
            subtitle = stringResource(R.string.study_reading_desc),
            badge = stringResource(R.string.study_reading_badge),
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            onClick = onOpenReading,
            testTag = "study_hub_reading_card"
        )

        StudyModuleOptionCard(
            title = stringResource(R.string.module_listening),
            subtitle = stringResource(R.string.study_listening_desc),
            badge = stringResource(R.string.study_listening_badge),
            icon = Icons.Outlined.Headphones,
            onClick = onOpenListening,
            testTag = "study_hub_listening_card"
        )
    }
}

@Composable
private fun StudyModuleOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    ContextaCard(
        onClick = onClick,
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                StatusPill(label = badge)
                Spacer(Modifier.height(6.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
