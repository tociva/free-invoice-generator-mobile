package com.example.freeinvoicegeneratorbydaybookcloud.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.freeinvoicegeneratorbydaybookcloud.data.template.RemoteInvoiceTemplate

internal data class InvoiceTemplateCatalogItem(
    val id: String,
    val title: String,
    val description: String,
    val accentColor: Color,
    val settingsContainerColor: Color,
    val headerColor: Color,
    val onHeaderColor: Color,
    val tableHeaderColor: Color,
    val cardCornerRadius: Dp,
    val layout: InvoiceTemplateLayout = InvoiceTemplateLayout.CLASSIC
)

internal enum class InvoiceTemplateLayout {
    CLASSIC,
    CENTERED,
    SIDE_RAIL,
    TOP_STRIPE,
    TOTAL_HERO,
    BOXED_META,
    MINIMAL,
    SPLIT_BAND,
    LEDGER,
    STUDIO,
    CORPORATE
}

internal fun invoiceTemplateCatalog(remoteTemplates: List<RemoteInvoiceTemplate> = emptyList()): List<InvoiceTemplateCatalogItem> {
    val curated = curatedInvoiceTemplateCatalog()
    val curatedIds = curated.map { it.id }.toSet()
    val remote = remoteTemplates
        .filterNot { template ->
            template.id in curatedIds || curated.any { it.title.equals(template.name, ignoreCase = true) }
        }
        .map { template ->
            val accent = templateAccent(template.id)
            InvoiceTemplateCatalogItem(
                id = template.id,
                title = template.name,
                description = template.description.ifBlank {
                    "${template.tags.joinToString(", ")} HTML template from Daybook.Cloud."
                },
                accentColor = accent,
                settingsContainerColor = accent.copy(alpha = 0.10f),
                headerColor = accent,
                onHeaderColor = Color.White,
                tableHeaderColor = accent.copy(alpha = 0.18f),
                cardCornerRadius = 12.dp
            )
        }
    return curated + remote
}

private fun curatedInvoiceTemplateCatalog() = listOf(
    InvoiceTemplateCatalogItem(
        id = "modern_teal",
        title = "Modern Teal",
        description = "Clean minimal layout matching Daybook.Cloud brand identity.",
        accentColor = Color(0xFF0F8F83),
        settingsContainerColor = Color(0xFFE6FFFB),
        headerColor = Color(0xFF0F8F83),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFCCFBF1),
        cardCornerRadius = 20.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "coral_breeze",
        title = "Coral Breeze",
        description = "Soft coral header with mint table highlights for a fresh simple invoice.",
        accentColor = Color(0xFFE85D5D),
        settingsContainerColor = Color(0xFFFFF1F1),
        headerColor = Color(0xFFE85D5D),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFCFFAF1),
        cardCornerRadius = 22.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "crimson_edge",
        title = "Crimson Edge",
        description = "Sharp red business styling with bold totals and clear table contrast.",
        accentColor = Color(0xFFE74C3C),
        settingsContainerColor = Color(0xFFFFF1F0),
        headerColor = Color(0xFFE74C3C),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFFDAD6),
        cardCornerRadius = 8.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "ruby_luxe",
        title = "Ruby Luxe",
        description = "Premium ruby-red invoice style for a stronger branded impression.",
        accentColor = Color(0xFFD81B60),
        settingsContainerColor = Color(0xFFFFEDF5),
        headerColor = Color(0xFFB5123E),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFFD6E4),
        cardCornerRadius = 18.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "violet_gradient",
        title = "Violet Gradient",
        description = "Modern violet palette inspired by the local gradient template family.",
        accentColor = Color(0xFF7C3AED),
        settingsContainerColor = Color(0xFFF5F3FF),
        headerColor = Color(0xFF6D28D9),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFEDE9FE),
        cardCornerRadius = 18.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "watercolor_gradient",
        title = "Watercolor Gradient",
        description = "Warm yellow-orange accents for friendly creative and service invoices.",
        accentColor = Color(0xFFE17055),
        settingsContainerColor = Color(0xFFFFF4DF),
        headerColor = Color(0xFFE17055),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFFE4B8),
        cardCornerRadius = 24.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "blue_split",
        title = "Blue Split",
        description = "Startup-style blue layout with strong sections and calm business tone.",
        accentColor = Color(0xFF2563EB),
        settingsContainerColor = Color(0xFFEFF6FF),
        headerColor = Color(0xFF1D4ED8),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFDBEAFE),
        cardCornerRadius = 14.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "inferno_line",
        title = "Inferno Line",
        description = "Orange-red accent style for energetic startup invoices.",
        accentColor = Color(0xFFF97316),
        settingsContainerColor = Color(0xFFFFF7ED),
        headerColor = Color(0xFFC2410C),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFFEDD5),
        cardCornerRadius = 12.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "poppins_breeze",
        title = "Poppins Breeze",
        description = "Soft blue rounded layout designed for readable GST invoices.",
        accentColor = Color(0xFF38BDF8),
        settingsContainerColor = Color(0xFFF0F9FF),
        headerColor = Color(0xFF0284C7),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFE0F2FE),
        cardCornerRadius = 24.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "bluecrest",
        title = "Bluecrest",
        description = "Tech-service palette with navy headings and soft blue-grey panels.",
        accentColor = Color(0xFF1E5AA7),
        settingsContainerColor = Color(0xFFEAF2FB),
        headerColor = Color(0xFF173B63),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFD7E6F5),
        cardCornerRadius = 10.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "elegant_gold",
        title = "Elegant Gold",
        description = "Corporate blue and gold styling for polished service invoices.",
        accentColor = Color(0xFFD4A017),
        settingsContainerColor = Color(0xFFFFF8E1),
        headerColor = Color(0xFF1F3A5F),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFFE8A3),
        cardCornerRadius = 12.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "modern_circle",
        title = "Modern Circle",
        description = "Contemporary indigo-teal look for modern GST billing.",
        accentColor = Color(0xFF14B8A6),
        settingsContainerColor = Color(0xFFF0FDFA),
        headerColor = Color(0xFF4338CA),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFCCFBF1),
        cardCornerRadius = 26.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "royal_plum",
        title = "Royal Plum",
        description = "Violet-plum professional style with a strong total highlight.",
        accentColor = Color(0xFF8B5CF6),
        settingsContainerColor = Color(0xFFF5F3FF),
        headerColor = Color(0xFF5B21B6),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFEDE9FE),
        cardCornerRadius = 18.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "teal_flow",
        title = "Teal Flow",
        description = "Elegant teal service template with clear structured sections.",
        accentColor = Color(0xFF009688),
        settingsContainerColor = Color(0xFFE0F7F5),
        headerColor = Color(0xFF00695C),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFB2DFDB),
        cardCornerRadius = 16.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "center_mark",
        title = "Center Mark",
        description = "Centered title layout with logo-first branding and balanced sections.",
        accentColor = Color(0xFF2563EB),
        settingsContainerColor = Color(0xFFEFF6FF),
        headerColor = Color(0xFF1E40AF),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFDBEAFE),
        cardCornerRadius = 18.dp,
        layout = InvoiceTemplateLayout.CENTERED
    ),
    InvoiceTemplateCatalogItem(
        id = "left_rail",
        title = "Left Rail",
        description = "Distinct colored rail on the left with compact invoice branding.",
        accentColor = Color(0xFF0891B2),
        settingsContainerColor = Color(0xFFECFEFF),
        headerColor = Color(0xFF0E7490),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFCFFAFE),
        cardCornerRadius = 10.dp,
        layout = InvoiceTemplateLayout.SIDE_RAIL
    ),
    InvoiceTemplateCatalogItem(
        id = "stripe_classic",
        title = "Stripe Classic",
        description = "Slim top stripe with a clean editorial invoice header.",
        accentColor = Color(0xFFEAB308),
        settingsContainerColor = Color(0xFFFEFCE8),
        headerColor = Color(0xFFA16207),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFEF3C7),
        cardCornerRadius = 6.dp,
        layout = InvoiceTemplateLayout.TOP_STRIPE
    ),
    InvoiceTemplateCatalogItem(
        id = "total_focus",
        title = "Total Focus",
        description = "Grand total appears in the header for quick customer scanning.",
        accentColor = Color(0xFF16A34A),
        settingsContainerColor = Color(0xFFF0FDF4),
        headerColor = Color(0xFF166534),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFDCFCE7),
        cardCornerRadius = 22.dp,
        layout = InvoiceTemplateLayout.TOTAL_HERO
    ),
    InvoiceTemplateCatalogItem(
        id = "boxed_meta",
        title = "Boxed Meta",
        description = "White header with invoice number and dates styled as boxed metadata.",
        accentColor = Color(0xFF7C2D12),
        settingsContainerColor = Color(0xFFFFF7ED),
        headerColor = Color(0xFF9A3412),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFFEDD5),
        cardCornerRadius = 12.dp,
        layout = InvoiceTemplateLayout.BOXED_META
    ),
    InvoiceTemplateCatalogItem(
        id = "minimal_letter",
        title = "Minimal Letter",
        description = "Letter-style invoice with quiet headings and no heavy color band.",
        accentColor = Color(0xFF334155),
        settingsContainerColor = Color(0xFFF8FAFC),
        headerColor = Color(0xFF334155),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFE2E8F0),
        cardCornerRadius = 2.dp,
        layout = InvoiceTemplateLayout.MINIMAL
    ),
    InvoiceTemplateCatalogItem(
        id = "split_brand",
        title = "Split Brand",
        description = "Split header with brand on one side and invoice identity on the other.",
        accentColor = Color(0xFFDB2777),
        settingsContainerColor = Color(0xFFFDF2F8),
        headerColor = Color(0xFFBE185D),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFCE7F3),
        cardCornerRadius = 20.dp,
        layout = InvoiceTemplateLayout.SPLIT_BAND
    ),
    InvoiceTemplateCatalogItem(
        id = "ledger_pro",
        title = "Ledger Pro",
        description = "Accounting-inspired layout with tighter spacing and ruled sections.",
        accentColor = Color(0xFF0F766E),
        settingsContainerColor = Color(0xFFF0FDFA),
        headerColor = Color(0xFF115E59),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFCCFBF1),
        cardCornerRadius = 4.dp,
        layout = InvoiceTemplateLayout.LEDGER
    ),
    InvoiceTemplateCatalogItem(
        id = "studio_card",
        title = "Studio Card",
        description = "Creative studio style with soft rounded blocks and prominent logo space.",
        accentColor = Color(0xFF9333EA),
        settingsContainerColor = Color(0xFFFAF5FF),
        headerColor = Color(0xFF7E22CE),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFF3E8FF),
        cardCornerRadius = 28.dp,
        layout = InvoiceTemplateLayout.STUDIO
    ),
    InvoiceTemplateCatalogItem(
        id = "corporate_panel",
        title = "Corporate Panel",
        description = "Formal panel header with strong business hierarchy and sober spacing.",
        accentColor = Color(0xFF1E3A8A),
        settingsContainerColor = Color(0xFFEFF6FF),
        headerColor = Color(0xFF172554),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFDBEAFE),
        cardCornerRadius = 8.dp,
        layout = InvoiceTemplateLayout.CORPORATE
    ),
    InvoiceTemplateCatalogItem(
        id = "classic_business",
        title = "Classic Business",
        description = "Traditional invoice layout with crisp sections and clear totals.",
        accentColor = Color(0xFF374151),
        settingsContainerColor = Color(0xFFF3F4F6),
        headerColor = Color(0xFF1F2937),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFE5E7EB),
        cardCornerRadius = 8.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "minimal_black",
        title = "Minimal Black",
        description = "High-contrast monochrome design for a formal professional look.",
        accentColor = Color(0xFF111827),
        settingsContainerColor = Color(0xFFF8FAFC),
        headerColor = Color(0xFF111827),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFF3F4F6),
        cardCornerRadius = 2.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "soft_green",
        title = "Soft Green",
        description = "Fresh, friendly layout with calm green highlight panels.",
        accentColor = Color(0xFF059669),
        settingsContainerColor = Color(0xFFECFDF5),
        headerColor = Color(0xFF047857),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFD1FAE5),
        cardCornerRadius = 20.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "premium_gold",
        title = "Premium Gold",
        description = "Warm gold accents for premium services and boutique brands.",
        accentColor = Color(0xFFB45309),
        settingsContainerColor = Color(0xFFFFFBEB),
        headerColor = Color(0xFF78350F),
        onHeaderColor = Color(0xFFFFFBEB),
        tableHeaderColor = Color(0xFFFEF3C7),
        cardCornerRadius = 14.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "corporate_slate",
        title = "Corporate Slate",
        description = "Dense, structured layout for B2B invoices and formal records.",
        accentColor = Color(0xFF475569),
        settingsContainerColor = Color(0xFFF1F5F9),
        headerColor = Color(0xFF334155),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFE2E8F0),
        cardCornerRadius = 10.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "creative_coral",
        title = "Creative Coral",
        description = "Modern coral highlights for creative studios and freelancers.",
        accentColor = Color(0xFFF43F5E),
        settingsContainerColor = Color(0xFFFFF1F2),
        headerColor = Color(0xFFE11D48),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFFFE4E6),
        cardCornerRadius = 22.dp
    ),
    InvoiceTemplateCatalogItem(
        id = "clean_ledger",
        title = "Clean Ledger",
        description = "Compact ledger-inspired format optimized for item-heavy invoices.",
        accentColor = Color(0xFF0D9488),
        settingsContainerColor = Color(0xFFF0FDFA),
        headerColor = Color(0xFF0F766E),
        onHeaderColor = Color.White,
        tableHeaderColor = Color(0xFFCCFBF1),
        cardCornerRadius = 6.dp
    )
)

private fun templateAccent(id: String): Color {
    val palette = listOf(
        Color(0xFF0F766E),
        Color(0xFF2563EB),
        Color(0xFFE11D48),
        Color(0xFF7C3AED),
        Color(0xFFB45309),
        Color(0xFF059669),
        Color(0xFFDC2626),
        Color(0xFF0891B2),
        Color(0xFF4F46E5),
        Color(0xFF475569)
    )
    return palette[id.hashCode().ushr(1) % palette.size]
}

internal fun InvoiceTemplateCatalogItem.matchesSearch(query: String): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return true
    return listOf(
        id,
        title,
        description,
        layout.name.replace('_', ' ')
    ).any { value ->
        value.contains(normalizedQuery, ignoreCase = true)
    }
}
