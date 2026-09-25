package com.example.freeinvoicegeneratorbydaybookcloud.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.freeinvoicegeneratorbydaybookcloud.pdf.InvoiceHtmlRenderer
import com.example.freeinvoicegeneratorbydaybookcloud.ui.components.DaybookTopBar
import com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.CreateInvoiceViewModel
import com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.InvoicePreviewEvent
import com.example.freeinvoicegeneratorbydaybookcloud.util.formatMoney
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.InvoiceType
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.PaymentMethod
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.TaxOption
import com.example.freeinvoicegeneratorbydaybookcloud.pdf.InvoicePdfGenerator
import com.example.freeinvoicegeneratorbydaybookcloud.util.InvoiceDownloadNotifier
import com.example.freeinvoicegeneratorbydaybookcloud.util.LogoResolver
import com.example.freeinvoicegeneratorbydaybookcloud.util.amountInWords

@Composable
fun InvoicePreviewScreen(
    invoiceId: Long,
    viewModel: CreateInvoiceViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val pdfActionState by viewModel.pdfActionState.collectAsStateWithLifecycle()
    val selectedTemplateId by viewModel.selectedTemplateId.collectAsStateWithLifecycle()
    val catalogLoaded by viewModel.templateCatalogLoaded.collectAsStateWithLifecycle()
    val templateLoading by viewModel.templatePageLoading.collectAsStateWithLifecycle()
    val templateError by viewModel.templatePageError.collectAsStateWithLifecycle()
    val templateLoaded = viewModel.isTemplateLoaded(selectedTemplateId)
    LaunchedEffect(selectedTemplateId, catalogLoaded, templateLoading) {
        if (catalogLoaded && !templateLoading && !templateLoaded) {
            viewModel.loadTemplatePage(listOf(selectedTemplateId))
        }
    }
    val templateStyle = invoicePreviewTemplateStyle(selectedTemplateId)
    val invoice = invoices.firstOrNull { it.id == invoiceId }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var pendingDownloadNotificationUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val uri = pendingDownloadNotificationUri
        pendingDownloadNotificationUri = null
        if (granted && uri != null) {
            InvoiceDownloadNotifier.showDownloaded(context, uri)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.previewEvents.collect { event ->
            when (event) {
                is InvoicePreviewEvent.DownloadSuccess -> {
                    Toast.makeText(context, "Invoice downloaded successfully", Toast.LENGTH_SHORT).show()
                    if (InvoiceDownloadNotifier.canShowNotification(context)) {
                        InvoiceDownloadNotifier.showDownloaded(context, event.uri)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pendingDownloadNotificationUri = event.uri
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                InvoicePreviewEvent.DownloadError -> {
                    Toast.makeText(context, "Unable to download invoice", Toast.LENGTH_SHORT).show()
                }
                is InvoicePreviewEvent.ShareReady -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = InvoicePdfGenerator.PDF_MIME_TYPE
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        putExtra(Intent.EXTRA_TITLE, event.fileName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Share invoice"))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No apps are available to share this invoice.", Toast.LENGTH_SHORT).show()
                    }
                }
                InvoicePreviewEvent.ShareError -> {
                    Toast.makeText(context, "Unable to share invoice", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Invoice?") },
            text = { Text("This action cannot be undone. The invoice and all its items will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (invoice == null) {
        Scaffold(
            topBar = {
                DaybookTopBar(
                    title = "Invoice",
                    onNavigationClick = onBack,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
        return
    }

    Scaffold(
        topBar = {
            DaybookTopBar(
                title = "Invoice Preview",
                onNavigationClick = onBack,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.shareInvoicePdf(invoiceId) },
                        enabled = !pdfActionState.isGeneratingPdf
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PreviewActionButton(
                        icon = Icons.Default.Download,
                        label = if (pdfActionState.isGeneratingPdf) "Generating PDF..." else "Download",
                        enabled = !pdfActionState.isGeneratingPdf,
                        onClick = { viewModel.downloadInvoicePdf(invoiceId) },
                        modifier = Modifier.weight(1f)
                    )
                    PreviewActionButton(
                        icon = Icons.Default.Share,
                        label = if (pdfActionState.isGeneratingPdf) "Generating PDF..." else "Share",
                        enabled = !pdfActionState.isGeneratingPdf,
                        onClick = { viewModel.shareInvoicePdf(invoiceId) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (!catalogLoaded || templateLoading || !templateLoaded) {
                if (templateError != null) {
                    Text(templateError!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.retryTemplatePage(listOf(selectedTemplateId)) }) {
                        Text("Retry template")
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("Loading invoice template…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                HtmlInvoicePreview(
                    invoice = invoice,
                    templateId = selectedTemplateId,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (false) {
            // ── Invoice Document Card ─────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(templateStyle.cardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = templateStyle.documentColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    InvoicePreviewHeader(invoice = invoice, templateStyle = templateStyle)

                    // Document body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // From / To row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FROM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = invoice.organizationName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = invoice.organizationAddress,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (invoice.invoiceType == InvoiceType.ADVANCED) {
                                    PreviewDetail(invoice.organizationCountry)
                                    PreviewDetail(invoice.organizationEmail)
                                    PreviewDetail(invoice.organizationMobile)
                                    PreviewDetail(invoice.organizationGstin, "GSTIN: ")
                                    PreviewDetail(listOf(invoice.authorityName, invoice.authorityDesignation).filter { it.isNotBlank() }.joinToString(", "))
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "BILL TO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = invoice.customerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = invoice.customerAddress,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End
                                )
                                if (invoice.invoiceType == InvoiceType.ADVANCED) {
                                    PreviewDetail(invoice.customerCountry, alignEnd = true)
                                    PreviewDetail(invoice.customerMobile, alignEnd = true)
                                    PreviewDetail(invoice.customerEmail, alignEnd = true)
                                    PreviewDetail(invoice.customerGstin, "GSTIN: ", true)
                                }
                            }
                        }

                        // Dates row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DateBadge(label = "Issue Date", value = invoice.date, modifier = Modifier.weight(1f))
                            DateBadge(label = "Due Date", value = invoice.dueDate, modifier = Modifier.weight(1f))
                        }
                        if (invoice.invoiceType == InvoiceType.ADVANCED && invoice.deliveryState.isNotBlank()) {
                            PreviewDetail(invoice.deliveryState, "Delivery State: ")
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.8.dp
                        )

                        // Items table
                        Column {
                            // Header
                            Text(
                                text = "ITEMS",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        templateStyle.tableHeaderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            invoice.items.forEachIndexed { index, item ->
                                val bg = if (index % 2 == 0) Color.Transparent
                                         else MaterialTheme.colorScheme.surfaceContainerLowest
                                InvoicePreviewRow(
                                    description = if (invoice.showItemDescription && item.description.isNotBlank()) "${item.name}\n${item.description}" else item.name,
                                    qty = item.quantity,
                                    unitPriceMinor = item.unitPriceMinor,
                                    totalMinor = item.totalMinor,
                                    currencySymbol = invoice.currencySymbol,
                                    decimalPlaces = invoice.decimalPlaces,
                                    internationalNumbering = invoice.internationalNumbering,
                                    bgColor = bg
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.8.dp
                        )

                        // Totals
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val money: (Long) -> String = { formatMoney(it, invoice.currencySymbol, invoice.decimalPlaces, invoice.internationalNumbering) }
                            TotalRow("Subtotal", money(invoice.subtotalMinor))
                            if (invoice.discountMinor != 0L) TotalRow("Discount", "- ${money(invoice.discountMinor)}")
                            if (invoice.invoiceType == InvoiceType.ADVANCED && invoice.taxAmountMinor != 0L) {
                                when (invoice.taxOption) {
                                    TaxOption.CGST_SGST -> {
                                        val cgst = invoice.taxAmountMinor / 2
                                        val sgst = invoice.taxAmountMinor - cgst
                                        TotalRow("CGST (${invoice.taxRatePercent / 2}%)", money(cgst))
                                        TotalRow("SGST (${invoice.taxRatePercent / 2}%)", money(sgst))
                                    }
                                    TaxOption.IGST -> TotalRow("IGST (${invoice.taxRatePercent}%)", money(invoice.taxAmountMinor))
                                    TaxOption.NON_TAXABLE -> Unit
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grand Total",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = money(invoice.totalMinor),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = templateStyle.accentColor
                                )
                            }
                            if (invoice.roundOffMinor != 0L) TotalRow("Round Off", money(invoice.roundOffMinor))
                            PreviewTextBlock(
                                "AMOUNT IN WORDS",
                                amountInWords(invoice.totalMinor, invoice.currencyCode, invoice.decimalPlaces),
                                valueFontSizeSp = 14
                            )
                        }

                        if (invoice.invoiceType == InvoiceType.ADVANCED && invoice.paymentDetails?.paymentMethod != PaymentMethod.NONE) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("PAYMENT DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                PreviewDetail(invoice.paymentDetails?.paymentMethod?.name.orEmpty(), "Method: ")
                                PreviewDetail(invoice.paymentDetails?.accountOwnerName.orEmpty(), "Account Owner: ")
                                PreviewDetail(invoice.paymentDetails?.accountNumber.orEmpty(), "Account: ")
                                PreviewDetail(invoice.paymentDetails?.bankName.orEmpty(), "Bank: ")
                                PreviewDetail(invoice.paymentDetails?.upiId.orEmpty(), "UPI: ")
                            }
                        }
                        if (invoice.additionalNotes.isNotBlank()) PreviewTextBlock("NOTES", invoice.additionalNotes)
                        if (invoice.termsAndConditions.isNotBlank()) PreviewTextBlock("TERMS AND CONDITIONS", invoice.termsAndConditions)

                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HtmlInvoicePreview(
    invoice: com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.InvoiceUiModel,
    templateId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoResolver = remember { LogoResolver() }
    val html = remember(invoice, templateId) {
        InvoiceHtmlRenderer.render(context, logoResolver, invoice, templateId)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(760.dp),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.textZoom = 100
                    settings.blockNetworkLoads = true
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    isHorizontalScrollBarEnabled = false
                    isVerticalScrollBarEnabled = false
                    setBackgroundColor(android.graphics.Color.WHITE)
                    webViewClient = WebViewClient()
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
            update = { webView ->
                val document = templateId to html
                if (webView.tag != document) {
                    webView.tag = document
                    webView.loadDataWithBaseURL(
                        InvoiceHtmlRenderer.baseUrl(templateId),
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
    }
}


private data class InvoicePreviewTemplateStyle(
    val headerColor: Color,
    val onHeaderColor: Color,
    val accentColor: Color,
    val documentColor: Color,
    val tableHeaderColor: Color,
    val cardCornerRadius: Dp,
    val layout: InvoiceTemplateLayout
)

@Composable
private fun InvoicePreviewHeader(
    invoice: com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.InvoiceUiModel,
    templateStyle: InvoicePreviewTemplateStyle
) {
    val topShape = RoundedCornerShape(
        topStart = templateStyle.cardCornerRadius,
        topEnd = templateStyle.cardCornerRadius
    )
    val logo: @Composable (Modifier, Alignment) -> Unit = { modifier, alignment ->
        invoice.organizationLogoPath?.let {
            OrganizationLogo(uri = it, modifier = modifier, contentAlignment = alignment)
        }
    }

    when (templateStyle.layout) {
        InvoiceTemplateLayout.CENTERED -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.headerColor, topShape)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                logo(Modifier.widthIn(max = 86.dp).height(46.dp), Alignment.Center)
                Spacer(Modifier.height(10.dp))
                Text("INVOICE", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.onHeaderColor, letterSpacing = 2.sp)
                Text("# ${invoice.invoiceNumber}", fontSize = 13.sp, color = templateStyle.onHeaderColor.copy(alpha = 0.82f))
            }
        }
        InvoiceTemplateLayout.SIDE_RAIL -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.documentColor, topShape)
                    .padding(end = 18.dp, top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(78.dp)
                        .background(templateStyle.headerColor, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("INVOICE", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.headerColor, letterSpacing = 1.sp)
                    Text("# ${invoice.invoiceNumber}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                logo(Modifier.widthIn(max = 86.dp).height(48.dp), Alignment.CenterEnd)
            }
        }
        InvoiceTemplateLayout.TOP_STRIPE, InvoiceTemplateLayout.LEDGER -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.documentColor, topShape)
            ) {
                Box(Modifier.fillMaxWidth().height(if (templateStyle.layout == InvoiceTemplateLayout.LEDGER) 6.dp else 12.dp).background(templateStyle.headerColor))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("INVOICE", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.headerColor)
                        Text("# ${invoice.invoiceNumber}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    logo(Modifier.widthIn(max = 88.dp).height(50.dp), Alignment.CenterEnd)
                }
            }
        }
        InvoiceTemplateLayout.TOTAL_HERO -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.headerColor, topShape)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("INVOICE", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.onHeaderColor)
                    Text("# ${invoice.invoiceNumber}", fontSize = 12.sp, color = templateStyle.onHeaderColor.copy(alpha = 0.78f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = templateStyle.onHeaderColor.copy(alpha = 0.72f))
                    Text(
                        formatMoney(invoice.totalMinor, invoice.currencySymbol, invoice.decimalPlaces, invoice.internationalNumbering),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = templateStyle.onHeaderColor,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
        InvoiceTemplateLayout.BOXED_META -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.documentColor, topShape)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("INVOICE", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.headerColor)
                    Text(invoice.organizationName.ifBlank { "Business" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(
                    modifier = Modifier
                        .background(templateStyle.tableHeaderColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("# ${invoice.invoiceNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = templateStyle.headerColor)
                    Text(invoice.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        InvoiceTemplateLayout.MINIMAL -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.documentColor, topShape)
                    .padding(20.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    logo(Modifier.widthIn(max = 72.dp).height(44.dp), Alignment.CenterStart)
                    Text("# ${invoice.invoiceNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Text("Invoice", fontSize = 26.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface)
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(1.dp).background(templateStyle.headerColor.copy(alpha = 0.35f)))
            }
        }
        InvoiceTemplateLayout.SPLIT_BAND -> {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(templateStyle.headerColor, RoundedCornerShape(topStart = templateStyle.cardCornerRadius))
                        .padding(18.dp)
                ) {
                    Column {
                        Text("INVOICE", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.onHeaderColor)
                        Text("# ${invoice.invoiceNumber}", fontSize = 12.sp, color = templateStyle.onHeaderColor.copy(alpha = 0.8f))
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(templateStyle.tableHeaderColor, RoundedCornerShape(topEnd = templateStyle.cardCornerRadius))
                        .padding(18.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    logo(Modifier.widthIn(max = 90.dp).height(50.dp), Alignment.CenterEnd)
                }
            }
        }
        InvoiceTemplateLayout.STUDIO -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.tableHeaderColor, topShape)
                    .padding(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    logo(Modifier.widthIn(max = 82.dp).height(46.dp), Alignment.CenterStart)
                    Surface(color = templateStyle.headerColor, shape = RoundedCornerShape(50)) {
                        Text("# ${invoice.invoiceNumber}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = templateStyle.onHeaderColor, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("INVOICE", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.headerColor)
            }
        }
        InvoiceTemplateLayout.CORPORATE -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.headerColor, topShape)
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(invoice.organizationName.ifBlank { "Business" }, fontSize = 13.sp, color = templateStyle.onHeaderColor.copy(alpha = 0.76f), fontWeight = FontWeight.Bold)
                        Text("INVOICE", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.onHeaderColor)
                    }
                    logo(Modifier.widthIn(max = 86.dp).height(48.dp), Alignment.CenterEnd)
                }
                Box(Modifier.fillMaxWidth().background(templateStyle.tableHeaderColor).padding(horizontal = 18.dp, vertical = 8.dp)) {
                    Text("# ${invoice.invoiceNumber}  |  ${invoice.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        InvoiceTemplateLayout.CLASSIC -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(templateStyle.headerColor, topShape)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("INVOICE", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = templateStyle.onHeaderColor, letterSpacing = 2.sp)
                        Text("# ${invoice.invoiceNumber}", fontSize = 13.sp, color = templateStyle.onHeaderColor.copy(alpha = 0.8f))
                    }
                    logo(Modifier.widthIn(max = 92.dp).height(54.dp), Alignment.CenterEnd)
                }
            }
        }
    }
}

@Composable
private fun invoicePreviewTemplateStyle(templateId: String): InvoicePreviewTemplateStyle {
    val scheme = MaterialTheme.colorScheme
    val normalizedTemplateId = when (templateId) {
        "elegant_blue" -> "blue_split"
        "royal_purple" -> "royal_plum"
        else -> templateId
    }
    val catalogTemplate = invoiceTemplateCatalog().firstOrNull { it.id == normalizedTemplateId }
    return if (catalogTemplate != null) {
        InvoicePreviewTemplateStyle(
            headerColor = catalogTemplate.headerColor,
            onHeaderColor = catalogTemplate.onHeaderColor,
            accentColor = catalogTemplate.accentColor,
            documentColor = scheme.surface,
            tableHeaderColor = catalogTemplate.tableHeaderColor,
            cardCornerRadius = catalogTemplate.cardCornerRadius,
            layout = catalogTemplate.layout
        )
    } else {
        InvoicePreviewTemplateStyle(
            headerColor = scheme.primary,
            onHeaderColor = scheme.onPrimary,
            accentColor = scheme.primary,
            documentColor = scheme.surface,
            tableHeaderColor = scheme.surfaceContainerLow,
            cardCornerRadius = 20.dp,
            layout = InvoiceTemplateLayout.CLASSIC
        )
    }
}

@Composable
private fun OrganizationLogo(
    uri: String,
    modifier: Modifier = Modifier
        .widthIn(max = 96.dp)
        .height(64.dp),
    contentAlignment: Alignment = Alignment.CenterStart
) {
    val context = LocalContext.current
    val logoResolver = remember { LogoResolver() }
    val bitmap = remember(uri) {
        logoResolver.decode(context, uri)
    }

    if (bitmap != null) {
        Box(
            modifier = modifier,
            contentAlignment = contentAlignment
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Organization logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun DateBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InvoicePreviewRow(
    description: String,
    qty: Int,
    unitPriceMinor: Long,
    totalMinor: Long,
    currencySymbol: String,
    decimalPlaces: Int,
    internationalNumbering: Boolean,
    bgColor: Color = Color.Transparent
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = description,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            ItemAmountCell(
                label = "Qty",
                value = qty.toString(),
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.Start
            )
            ItemAmountCell(
                label = "Price",
                value = formatMoney(unitPriceMinor, currencySymbol, decimalPlaces, internationalNumbering),
                modifier = Modifier.weight(1.45f),
                textAlign = TextAlign.End
            )
            ItemAmountCell(
                label = "Total",
                value = formatMoney(totalMinor, currencySymbol, decimalPlaces, internationalNumbering),
                modifier = Modifier.weight(1.45f),
                textAlign = TextAlign.End,
                emphasize = true
            )
        }
    }
}

@Composable
private fun ItemAmountCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    emphasize: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = when (textAlign) {
            TextAlign.End -> Alignment.End
            TextAlign.Center -> Alignment.CenterHorizontally
            else -> Alignment.Start
        },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = if (emphasize) 13.sp else 12.sp,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PreviewDetail(value: String, prefix: String = "", alignEnd: Boolean = false) {
    if (value.isBlank()) return
    Text(
        text = prefix + value,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

@Composable
private fun PreviewTextBlock(label: String, value: String, valueFontSizeSp: Int = 12) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = valueFontSizeSp.sp)
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PreviewActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
