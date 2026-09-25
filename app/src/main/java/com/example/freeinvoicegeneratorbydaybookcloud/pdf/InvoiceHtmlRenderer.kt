package com.example.freeinvoicegeneratorbydaybookcloud.pdf

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.freeinvoicegeneratorbydaybookcloud.data.template.RemoteInvoiceTemplateStore
import com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.InvoiceUiModel
import com.example.freeinvoicegeneratorbydaybookcloud.util.LogoResolver
import com.example.freeinvoicegeneratorbydaybookcloud.util.amountInWords
import com.example.freeinvoicegeneratorbydaybookcloud.util.formatMoney
import java.io.ByteArrayOutputStream

internal object InvoiceHtmlRenderer {
    fun baseUrl(templateId: String): String {
        RemoteInvoiceTemplateStore.pathFor(templateId)?.let { path ->
            return "https://free-invoice-generator-dev.daybook.cloud/${path.substringBeforeLast('/')}/"
        }
        return "https://free-invoice-generator-dev.daybook.cloud/"
    }

    fun render(
        context: Context,
        logoResolver: LogoResolver,
        invoice: InvoiceUiModel,
        templateId: String
    ): String {
        val template = RemoteInvoiceTemplateStore.htmlFor(templateId)
            ?: error("Invoice template is not available. Connect to the internet and try again.")
        val logoSrc = logoDataUri(context, logoResolver, invoice.organizationLogoPath)
        val replacements = mapOf(
            "logo_small_src" to logoSrc,
            "org_name" to invoice.organizationName,
            "org_address" to invoice.organizationAddress,
            "org_country" to invoice.organizationCountry,
            "org_phone" to invoice.organizationMobile,
            "org_mobile" to invoice.organizationMobile,
            "org_email" to invoice.organizationEmail,
            "org_gstin" to invoice.organizationGstin,
            "authority_name" to invoice.authorityName,
            "authority_designation" to invoice.authorityDesignation,
            "customer_name" to invoice.customerName,
            "customer_address" to invoice.customerAddress,
            "customer_country" to invoice.customerCountry,
            "customer_phone" to invoice.customerMobile,
            "customer_mobile" to invoice.customerMobile,
            "customer_email" to invoice.customerEmail,
            "customer_gstin" to invoice.customerGstin,
            "invoice_number" to invoice.invoiceNumber,
            "invoice_date" to invoice.date,
            "payment_due_date" to invoice.dueDate,
            "due_date" to invoice.dueDate,
            "itemtotal" to money(invoice, invoice.items.sumOf { it.lineSubtotalMinor }),
            "subtotal" to money(invoice, invoice.subtotalMinor),
            "discount" to money(invoice, invoice.discountMinor),
            "tax_amount" to money(invoice, invoice.taxAmountMinor),
            "roundoff" to money(invoice, invoice.roundOffMinor),
            "grand_total" to money(invoice, invoice.totalMinor),
            "amount_in_words" to amountInWords(invoice.totalMinor, invoice.currencyCode, invoice.decimalPlaces),
            "notes" to invoice.additionalNotes,
            "terms_and_conditions" to invoice.termsAndConditions,
            "payment_method" to invoice.paymentDetails?.paymentMethod?.name.orEmpty(),
            "account_number" to invoice.paymentDetails?.accountNumber.orEmpty(),
            "account_name" to invoice.paymentDetails?.accountOwnerName.orEmpty(),
            "bank_name" to invoice.paymentDetails?.bankName.orEmpty(),
            "upi_id" to invoice.paymentDetails?.upiId.orEmpty()
        )
        return replaceItemBlock(template.removeExternalResources(), invoice)
            .replaceTokens(replacements)
            .replace(Regex("""\[\[[A-Za-z0-9_]+]]"""), "")
            .withDocumentStyle()
    }

    private fun String.withDocumentStyle(): String {
        // Keep an A4 layout in both outputs; the preview scales the page to the phone.
        val style = """
            <meta name="viewport" content="width=794">
            <style>
              @page { size: A4; margin: 0; }
              html { margin: 0 !important; padding: 0 !important; background: white; }
              body {
                width: 210mm !important; max-width: 210mm !important; min-width: 0 !important;
                margin: 0 !important; box-sizing: border-box !important;
              }
              *, *::before, *::after {
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
              }
              .invoice-container, .container, .invoice, .invoice-box, .page, .wrapper, main {
                max-width: 100% !important; min-width: 0 !important;
                box-sizing: border-box !important;
              }
              img { max-width: 100%; object-fit: contain; }
              img[src^="data:image"] { max-width: 180px; max-height: 90px; }
              img[src=""] { display: none; }
              table { max-width: 100%; }
              th, td { overflow-wrap: anywhere; }
              tr, img { break-inside: avoid; }
            </style>
        """.trimIndent()
        val document = replace(
            Regex("""<meta\b[^>]*name\s*=\s*["']viewport["'][^>]*>""", RegexOption.IGNORE_CASE), ""
        )
        val headEnd = Regex("</head>", RegexOption.IGNORE_CASE)
        return if (headEnd.containsMatchIn(document)) {
            headEnd.replace(document) { "$style</head>" }
        } else {
            "$style$document"
        }
    }

    private fun replaceItemBlock(template: String, invoice: InvoiceUiModel): String {
        val regex = Regex("""(?s)\[\[items_start]](.*?)\[\[items_end]]""")
        return regex.replace(template) { match ->
            val rowTemplate = match.groupValues[1]
            invoice.items.joinToString("") { item ->
                val cgst = if (item.cgstPercent != 0.0) "${item.cgstPercent}%" else ""
                val sgst = if (item.sgstPercent != 0.0) "${item.sgstPercent}%" else ""
                val igst = if (item.igstPercent != 0.0) "${item.igstPercent}%" else ""
                rowTemplate.replaceTokens(
                    mapOf(
                        "item_name" to buildString {
                            append(item.name.escapeHtml())
                            if (invoice.showItemDescription && item.description.isNotBlank()) {
                                append("<br><small>")
                                append(item.description.escapeHtml())
                                append("</small>")
                            }
                        },
                        "item_description" to item.description,
                        "item_quantity" to item.quantity.toString(),
                        "item_price" to money(invoice, item.unitPriceMinor),
                        "item_cgst" to cgst,
                        "item_sgst" to sgst,
                        "item_igst" to igst,
                        "item_tax" to money(invoice, item.taxAmountMinor),
                        "item_discount" to money(invoice, item.discountMinor),
                        "item_amount" to money(invoice, item.totalMinor),
                        "item_total" to money(invoice, item.totalMinor)
                    ),
                    escape = false
                )
            }
        }
    }

    private fun String.replaceTokens(values: Map<String, String>, escape: Boolean = true): String {
        var output = this
        values.forEach { (key, value) ->
            output = output.replace("[[$key]]", if (escape) value.escapeHtml() else value)
        }
        return output
    }

    private fun logoDataUri(context: Context, logoResolver: LogoResolver, logoReference: String?): String {
        val logo = logoResolver.decode(context, logoReference) ?: return ""
        return runCatching {
            val output = ByteArrayOutputStream()
            logo.compress(Bitmap.CompressFormat.PNG, 100, output)
            "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }.getOrDefault("")
    }

    private fun money(invoice: InvoiceUiModel, amount: Long): String =
        formatMoney(amount, invoice.currencySymbol, invoice.decimalPlaces, invoice.internationalNumbering)

    private fun String.removeExternalResources(): String =
        replace(Regex("""<link\b[^>]*https?://[^>]*>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<script\b[^>]*https?://.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")

    private fun String.escapeHtml(): String = buildString {
        this@escapeHtml.forEach { char ->
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&#39;"
                    else -> char
                }
            )
        }
    }
}
