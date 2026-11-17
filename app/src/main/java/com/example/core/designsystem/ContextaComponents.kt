package com.example.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.model.SyncStatus

@Composable
fun ContextaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalContextaColors.current
    val shape = MaterialTheme.shapes.medium
    val colors = CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    val border = BorderStroke(1.dp, ext.cardBorder)
    val cardModifier = modifier.shadow(
        elevation = 8.dp,
        shape = shape,
        ambientColor = ext.cardShadow,
        spotColor = ext.cardShadow
    )
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border
        ) {
            Column(modifier = Modifier.padding(18.dp), content = content)
        }
    } else {
        OutlinedCard(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border
        ) {
            Column(modifier = Modifier.padding(18.dp), content = content)
        }
    }
}

@Composable
fun ContextaNavyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalContextaColors.current
    val shape = MaterialTheme.shapes.large
    val colors = CardDefaults.outlinedCardColors(
        containerColor = ext.navySurface,
        contentColor = ext.onNavySurface
    )
    val border = BorderStroke(1.dp, ext.navyBorder)
    val cardModifier = modifier.shadow(
        elevation = 12.dp,
        shape = shape,
        ambientColor = ext.cardShadow,
        spotColor = ext.cardShadow
    )
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    } else {
        OutlinedCard(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    icon: ImageVector? = null,
    isPositive: Boolean = false,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isPositive -> MaterialTheme.colorScheme.secondaryContainer
        isWarning -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        isPositive -> MaterialTheme.colorScheme.onSecondaryContainer
        isWarning -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
fun SyncBadge(
    syncStatus: SyncStatus,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    when {
        syncStatus == SyncStatus.DEMO_LOCAL -> StatusPill(
            label = stringResource(R.string.sync_badge_demo),
            icon = Icons.Outlined.Visibility,
            modifier = modifier
        )
        syncStatus == SyncStatus.PENDING_LOCAL || !isOnline -> StatusPill(
            label = stringResource(R.string.sync_badge_pending),
            icon = if (isOnline) Icons.Outlined.CloudQueue else Icons.Outlined.CloudOff,
            isWarning = true,
            modifier = modifier
        )
        else -> StatusPill(
            label = stringResource(R.string.sync_badge_synced),
            icon = Icons.Outlined.CloudDone,
            isPositive = true,
            modifier = modifier
        )
    }
}

@Composable
fun DemoModeBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.demo_mode_banner_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun ContextaLoadingView(
    message: String = stringResource(R.string.loading_default),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ContextaEmptyView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ContextaErrorView(
    message: String,
    retryLabel: String = stringResource(R.string.action_retry),
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text(retryLabel)
                }
            }
        }
    }
}
