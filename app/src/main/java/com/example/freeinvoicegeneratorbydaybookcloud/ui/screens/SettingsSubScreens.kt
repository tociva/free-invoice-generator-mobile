package com.example.freeinvoicegeneratorbydaybookcloud.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.freeinvoicegeneratorbydaybookcloud.ui.components.DaybookBottomNavigation
import com.example.freeinvoicegeneratorbydaybookcloud.ui.components.DaybookTopBar
import com.example.freeinvoicegeneratorbydaybookcloud.ui.components.PrimaryButton
import com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.SettingsViewModel
import com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.ThemeMode
import com.example.freeinvoicegeneratorbydaybookcloud.data.preferences.majorCurrencies
import com.example.freeinvoicegeneratorbydaybookcloud.util.LogoResolver
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun OrganizationSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val savedName by viewModel.organizationName.collectAsStateWithLifecycle()
    val savedAddress by viewModel.organizationAddress.collectAsStateWithLifecycle()
    val savedEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val savedPhone by viewModel.phoneNumber.collectAsStateWithLifecycle()
    val savedLogoPath by viewModel.organizationLogoPath.collectAsStateWithLifecycle()

    var name by remember(savedName) { mutableStateOf(savedName) }
    var address by remember(savedAddress) { mutableStateOf(savedAddress) }
    var email by remember(savedEmail) { mutableStateOf(savedEmail) }
    var phone by remember(savedPhone) { mutableStateOf(savedPhone) }
    var logoPath by remember(savedLogoPath) { mutableStateOf(savedLogoPath) }
    var phoneCountryCode by remember(savedPhone) { mutableStateOf(countryForValue(savedPhone).dialCode) }
    val validEmail = isValidEmail(email)
    val validPhone = isValidMobile(phone)
    val context = LocalContext.current
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            logoPath = it.toString()
            Toast.makeText(context, "Logo uploaded successfully", Toast.LENGTH_SHORT).show()
        }
    }

    SubScreenScaffold(title = "Organization Settings", onBack = onBack) {
        OutlinedButton(
            onClick = { logoPicker.launch(arrayOf("image/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Business, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (logoPath == null) "Upload Logo" else "Change Logo")
        }
        if (logoPath != null) {
            OrganizationSettingsLogoPreview(logoPath.orEmpty())
            TextButton(
                onClick = {
                    logoPath = null
                    Toast.makeText(context, "Logo removed", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove Logo")
            }
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Company Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        OutlinedTextField(
            value = address, onValueChange = { address = it },
            label = { Text("Business Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = 2
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email Address") },
            placeholder = { Text("name@example.com") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
        if (!validEmail) Text("Enter a valid email address.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SimpleChoiceMenu(
                label = "Code",
                value = phoneCountryCode,
                options = invoiceCountries.map { it.dialCode }.distinct(),
                modifier = Modifier.width(112.dp)
            ) { selected ->
                phone = combineMobile(selected, mobileNumberPart(phone, phoneCountryCode))
                phoneCountryCode = selected
            }
            OutlinedTextField(
                value = mobileNumberPart(phone, phoneCountryCode),
                onValueChange = { phone = combineMobile(phoneCountryCode, it) },
                label = { Text("Mobile Number") },
                placeholder = { Text("9876543210") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }
        if (!validPhone) Text("Enter a valid mobile number.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = "Save Changes",
            enabled = validEmail && validPhone,
            onClick = {
                viewModel.updateOrganization(name, address, email, phone, logoPath)
                onBack()
            }
        )
    }
}

@Composable
private fun OrganizationSettingsLogoPreview(uri: String) {
    val context = LocalContext.current
    val logoResolver = remember { LogoResolver() }
    val bitmap = remember(uri) { logoResolver.decode(context, uri) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Organization logo preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val savedSettings by viewModel.invoiceSettings.collectAsStateWithLifecycle()
    var prefix by remember(savedSettings.prefix) { mutableStateOf(savedSettings.prefix) }
    var taxRate by remember(savedSettings.taxRatePercent) { mutableStateOf(savedSettings.taxRatePercent.toString()) }
    var currencyCode by remember(savedSettings.currencyCode) { mutableStateOf(savedSettings.currencyCode) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val selectedCurrency = majorCurrencies.firstOrNull { it.code == currencyCode } ?: majorCurrencies.first()

    SubScreenScaffold(title = "Invoice Settings", onBack = onBack) {
        OutlinedTextField(
            value = prefix, onValueChange = { prefix = it },
            label = { Text("Invoice Number Prefix") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
        OutlinedTextField(
            value = taxRate,
            onValueChange = { value ->
                if (value.all(Char::isDigit) && value.length <= 3) taxRate = value
            },
            label = { Text("Default Tax Rate (%)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
        ExposedDropdownMenuBox(
            expanded = currencyExpanded,
            onExpandedChange = { currencyExpanded = !currencyExpanded }
        ) {
            OutlinedTextField(
                value = selectedCurrency.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Default Currency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = currencyExpanded,
                onDismissRequest = { currencyExpanded = false }
            ) {
                majorCurrencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency.displayName) },
                        onClick = {
                            currencyCode = currency.code
                            currencyExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = "Save Settings",
            enabled = prefix.isNotBlank() && (taxRate.toIntOrNull() in 0..100),
            onClick = {
                viewModel.updateInvoiceSettings(prefix, taxRate, currencyCode)
                onBack()
            }
        )
    }
}

@Composable
fun TemplatesScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    showBottomNavigation: Boolean = false,
    onTabSelected: (String) -> Unit = {}
) {
    val invoiceSettings by viewModel.invoiceSettings.collectAsStateWithLifecycle()
    val remoteTemplates by viewModel.remoteTemplates.collectAsStateWithLifecycle()
    var templateSearchQuery by remember { mutableStateOf("") }
    val templates = invoiceTemplateCatalog(remoteTemplates)
    val filteredTemplates = remember(templateSearchQuery, templates) {
        templates.filter { it.matchesSearch(templateSearchQuery) }
    }
    val pageSize = 10
    var loadedTemplateCount by remember { mutableIntStateOf(pageSize) }
    val visibleTemplates = filteredTemplates.take(loadedTemplateCount)
    val pageLoading by viewModel.templatePageLoading.collectAsStateWithLifecycle()
    val pageError by viewModel.templatePageError.collectAsStateWithLifecycle()
    val catalogLoaded by viewModel.templateCatalogLoaded.collectAsStateWithLifecycle()
    var displayMode by rememberSaveable { mutableStateOf(TemplateDisplayMode.LIST) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            if (displayMode == TemplateDisplayMode.GRID) {
                gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
            } else {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }
    }

    LaunchedEffect(templateSearchQuery, remoteTemplates) {
        loadedTemplateCount = pageSize
        listState.scrollToItem(0)
        gridState.scrollToItem(0)
    }
    LaunchedEffect(displayMode, catalogLoaded, filteredTemplates.size) {
        snapshotFlow {
            val lastVisibleIndex = if (displayMode == TemplateDisplayMode.GRID) {
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            } else {
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            }
            Triple(lastVisibleIndex, pageLoading, loadedTemplateCount)
        }.distinctUntilChanged().collect { (lastVisibleIndex, loading, loadedCount) ->
            if (catalogLoaded && !loading && loadedCount < filteredTemplates.size &&
                lastVisibleIndex >= loadedCount - 3
            ) {
                loadedTemplateCount = (loadedCount + pageSize).coerceAtMost(filteredTemplates.size)
            }
        }
    }
    LaunchedEffect(visibleTemplates, catalogLoaded) {
        if (catalogLoaded) viewModel.loadTemplatePage(visibleTemplates.map { it.id })
    }

    SubScreenScaffold(
        title = "Invoice Templates",
        onBack = onBack,
        scrollable = false,
        showTopBar = !showBottomNavigation,
        showBack = !showBottomNavigation,
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp),
        bottomBar = {
            if (showBottomNavigation) {
                DaybookBottomNavigation(
                    currentRoute = "templates",
                    onTabSelected = onTabSelected
                )
            }
        }
    ) {
        if (showBottomNavigation) {
            Text(
                text = "Invoice Templates",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Choose your preferred invoice layout template.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = if (showBottomNavigation) TextAlign.Center else TextAlign.Start,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = templateSearchQuery,
                onValueChange = { templateSearchQuery = it },
                modifier = Modifier.weight(1f).height(56.dp),
                singleLine = true,
                placeholder = { Text("Search templates") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (templateSearchQuery.isNotBlank()) {
                        IconButton(onClick = { templateSearchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear template search")
                        }
                    }
                }
            )
            TemplateDisplayToggle(displayMode) { displayMode = it }
        }
        if (!catalogLoaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Loading template catalog…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (filteredTemplates.isEmpty()) {
            Text(
                text = "No templates found.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            pageError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                TextButton(onClick = { viewModel.retryTemplatePage(visibleTemplates.map { it.id }) }) { Text("Retry") }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (displayMode == TemplateDisplayMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibleTemplates, key = { it.id }) { template ->
                            TemplateGridCard(
                                template = template,
                                selected = invoiceSettings.templateId == template.id,
                                onClick = { viewModel.selectInvoiceTemplate(template.id) },
                                enabled = !pageLoading
                            )
                        }
                        if (pageLoading && visibleTemplates.size < filteredTemplates.size) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(visibleTemplates, key = { it.id }) { template ->
                            TemplateOptionCard(
                                template = template,
                                selected = invoiceSettings.templateId == template.id,
                                onClick = { viewModel.selectInvoiceTemplate(template.id) },
                                enabled = !pageLoading
                            )
                        }
                        if (pageLoading && visibleTemplates.size < filteredTemplates.size) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
                TemplateScrollToTopButton(
                    visible = showScrollToTop,
                    onClick = {
                        coroutineScope.launch {
                            if (displayMode == TemplateDisplayMode.GRID) gridState.animateScrollToItem(0)
                            else listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun TemplateOptionCard(
    template: InvoiceTemplateCatalogItem,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = template.settingsContainerColor),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) template.accentColor else template.accentColor.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TemplatePreview(accent = template.accentColor, large = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (template.id == "modern_teal") "${template.title} (Default)" else template.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = template.description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (template.id == "modern_teal") {
                    Surface(
                        color = template.accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Default",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = template.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(selectedColor = template.accentColor)
            )
        }
    }
}

@Composable
fun AppearanceScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()

    SubScreenScaffold(title = "Appearance", onBack = onBack) {
        Text(
            text = "THEME PREFERENCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                ThemeOptionRow(
                    title = "System Default",
                    subtitle = "Match your device settings (${if (systemInDark) "Dark" else "Light"})",
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )
                ThemeOptionRow(
                    title = "Light Theme",
                    subtitle = "Always use light theme",
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )
                ThemeOptionRow(
                    title = "Dark Theme",
                    subtitle = "Always use dark theme",
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    SubScreenScaffold(title = "Help & Support", onBack = onBack) {
        Text(text = "Need help with Daybook.Cloud?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Daybook.Cloud is free, open source, and works locally on your device. " +
                "For support, documentation, or to contribute, visit our GitHub repository.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    SubScreenScaffold(title = "About", onBack = onBack) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Daybook.Cloud Mobile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version 1.0.0 (Local-First)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Create professional invoices instantly with zero signups and 100% data privacy.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared sub-screen scaffold
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    showBack: Boolean = true,
    scrollable: Boolean = true,
    showTopBar: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            if (showTopBar) {
                DaybookTopBar(
                    title = title,
                    onNavigationClick = if (showBack) onBack else null,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
                )
            }
        },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}
