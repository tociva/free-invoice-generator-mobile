package com.example.freeinvoicegeneratorbydaybookcloud.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.freeinvoicegeneratorbydaybookcloud.ui.components.DaybookBottomNavigation
import com.example.freeinvoicegeneratorbydaybookcloud.ui.components.EmptyState
import com.example.freeinvoicegeneratorbydaybookcloud.ui.theme.DarkNavy
import com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.HomeViewModel
import com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.InvoiceUiModel
import com.example.freeinvoicegeneratorbydaybookcloud.platform.LocalPlatformActions
import com.example.freeinvoicegeneratorbydaybookcloud.util.formatMoney
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onTabSelected: (String) -> Unit
) {
    val organizationName by viewModel.organizationName.collectAsStateWithLifecycle()
    val organizationLogoPath by viewModel.organizationLogoPath.collectAsStateWithLifecycle()
    val recentInvoices by viewModel.recentInvoices.collectAsStateWithLifecycle()
    val greeting = rememberTimeBasedGreeting()
    val displayName = organizationName?.takeIf { it.isNotBlank() } ?: "there"

    Scaffold(
        bottomBar = {
            DaybookBottomNavigation(
                currentRoute = "home",
                onTabSelected = onTabSelected
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HomeHeader(
                    avatarText = displayName.firstOrNull()?.uppercase().orEmpty().ifBlank { "D" },
                    logoPath = organizationLogoPath,
                    onAvatarClick = onNavigateToSettings
                )
            }

            item {
                WelcomeCard(
                    greeting = greeting,
                    organizationName = displayName
                )
            }

            item {
                SectionTitle(
                    title = "Recent Invoices",
                    actionText = "View All",
                    onActionClick = onNavigateToInvoices
                )
            }

            if (recentInvoices.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Description,
                        title = "No invoices yet",
                        description = "Create your first invoice to get started.",
                        actionText = "Create Invoice",
                        onActionClick = onNavigateToCreate
                    )
                }
            } else {
                item {
                    RecentInvoicesCard(
                        invoices = recentInvoices.take(3),
                        onNavigateToPreview = onNavigateToPreview
                    )
                }
            }

            item {
                TemplatesCard(onClick = onNavigateToTemplates)
            }

        }
    }
}

@Composable
private fun HomeHeader(
    avatarText: String,
    logoPath: String?,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Free Invoice Generator",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 28.sp
            )
            Text(
                text = "by Daybook.Cloud",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            val platform = LocalPlatformActions.current
            val bitmap = remember(logoPath) { platform.decodeLogo(logoPath) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Organization logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = avatarText,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun WelcomeCard(
    greeting: String,
    organizationName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE8F9F8),
                            Color(0xFFF4FCFF),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.64f)
            ) {
                Text(
                    text = greeting,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hello, $organizationName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkNavy,
                    lineHeight = 29.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Manage your invoices\nwith ease and focus on\nwhat matters most.",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            InvoiceHeroIllustration(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(118.dp)
            )
        }
    }
}

@Composable
private fun InvoiceHeroIllustration(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(108.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 82.dp, height = 108.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp), clip = false),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .height(if (index == 0) 8.dp else 6.dp)
                            .fillMaxWidth(if (index == 0) 0.86f else 0.62f + index * 0.1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth(0.7f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFFFFF2CC)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.StackedLineChart,
                contentDescription = null,
                tint = Color(0xFFB7791F),
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$actionText ->",
            modifier = Modifier.clickable(onClick = onActionClick),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RecentInvoicesCard(
    invoices: List<InvoiceUiModel>,
    onNavigateToPreview: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            invoices.forEachIndexed { index, invoice ->
                RecentInvoiceRow(
                    invoice = invoice,
                    onClick = { onNavigateToPreview(invoice.id) }
                )
                if (index < invoices.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentInvoiceRow(
    invoice: InvoiceUiModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = invoice.invoiceNumber,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = invoice.customerName.ifBlank { invoice.organizationName },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = invoice.date,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMoney(
                    invoice.amountMinor,
                    invoice.currencySymbol,
                    invoice.decimalPlaces,
                    invoice.internationalNumbering
                ),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TemplatesCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF7FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, top = 20.dp, end = 18.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TEMPLATES",
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Professional Templates",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkNavy,
                    lineHeight = 25.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "Create beautiful invoices\nin seconds.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewModule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberTimeBasedGreeting(): String {
    val hour = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    }
    return when {
        hour in 5..11 -> "GOOD MORNING"
        hour in 12..16 -> "GOOD AFTERNOON"
        hour in 17..20 -> "GOOD EVENING"
        else -> "GOOD EVENING"
    }
}

