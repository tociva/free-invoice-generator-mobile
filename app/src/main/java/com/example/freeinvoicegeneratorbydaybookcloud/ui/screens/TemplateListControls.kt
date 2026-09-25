package com.example.freeinvoicegeneratorbydaybookcloud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class TemplateDisplayMode {
    LIST,
    GRID
}

@Composable
internal fun TemplateDisplayToggle(
    mode: TemplateDisplayMode,
    onModeChange: (TemplateDisplayMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TemplateDisplayButton(
            selected = mode == TemplateDisplayMode.LIST,
            contentDescription = "List view",
            onClick = { onModeChange(TemplateDisplayMode.LIST) }
        ) {
            Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        TemplateDisplayButton(
            selected = mode == TemplateDisplayMode.GRID,
            contentDescription = "Grid view",
            onClick = { onModeChange(TemplateDisplayMode.GRID) }
        ) {
            Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TemplateDisplayButton(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .semantics { this.contentDescription = contentDescription }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ) {
            content()
        }
    }
}

@Composable
internal fun TemplateScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (visible) {
        SmallFloatingActionButton(
            onClick = onClick,
            modifier = modifier.semantics { contentDescription = "Scroll to top" },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = null)
        }
    }
}

@Composable
internal fun TemplateGridCard(
    template: InvoiceTemplateCatalogItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box {
        Card(
            modifier = Modifier.fillMaxWidth().height(188.dp),
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = template.settingsContainerColor),
            border = androidx.compose.foundation.BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) template.accentColor else template.accentColor.copy(alpha = 0.28f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TemplatePreview(accent = template.accentColor)
                Text(
                    text = template.title,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleSmall.fontSize,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = template.description,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = template.accentColor,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)
            )
        }
    }
}

@Composable
internal fun TemplatePreview(accent: Color, large: Boolean = false) {
    Column(
        modifier = Modifier
            .width(if (large) 80.dp else 58.dp)
            .height(if (large) 104.dp else 62.dp)
            .clip(RoundedCornerShape(if (large) 10.dp else 6.dp))
            .background(if (large) Color.White else Color.Transparent)
            .padding(if (large) 8.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (large) 14.dp else 8.dp)
                .background(accent, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
        )
        if (large) {
            Text(
                text = "INVOICE",
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
            )
        }
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 0) 0.85f else 0.62f + index * 0.12f)
                    .height(if (large) 5.dp else 5.dp)
                    .background(accent.copy(alpha = 0.20f), RoundedCornerShape(50))
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(7.dp)
                .align(Alignment.End)
                .background(accent.copy(alpha = 0.55f), RoundedCornerShape(50))
        )
    }
}
