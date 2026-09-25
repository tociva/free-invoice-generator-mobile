package com.example.freeinvoicegeneratorbydaybookcloud.pdf

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel.InvoiceUiModel
import com.example.freeinvoicegeneratorbydaybookcloud.data.template.RemoteInvoiceTemplateStore
import com.example.freeinvoicegeneratorbydaybookcloud.util.LogoResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvoicePdfGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logoResolver: LogoResolver
) {
    suspend fun createSharePdf(invoice: InvoiceUiModel, templateId: String): Result<File> = pdfResult {
        withContext(Dispatchers.IO) {
            RemoteInvoiceTemplateStore.load()
            RemoteInvoiceTemplateStore.loadHtml(listOf(templateId)).getOrThrow()
            val directory = File(context.cacheDir, "invoices").apply { mkdirs() }
            val file = File(directory, safeFileName(invoice.invoiceNumber))
            val html = InvoiceHtmlRenderer.render(context, logoResolver, invoice, templateId)
            try {
                HtmlPdfPrinter.write(context, html, InvoiceHtmlRenderer.baseUrl(templateId), file)
                file
            } catch (error: Exception) {
                file.delete()
                throw error
            }
        }
    }

    suspend fun saveToDownloads(invoice: InvoiceUiModel, templateId: String): Result<Uri> = pdfResult {
        val tempFile = createSharePdf(invoice, templateId).getOrThrow()
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeFileName(invoice.invoiceNumber))
                    put(MediaStore.Downloads.MIME_TYPE, PDF_MIME_TYPE)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Unable to create Downloads entry.")
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("Unable to open Downloads output stream.")
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri
                } catch (error: Exception) {
                    resolver.delete(uri, null, null)
                    throw error
                }
            } else {
                val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: error("Downloads directory is unavailable.")
                downloads.mkdirs()
                val target = File(downloads, tempFile.name)
                tempFile.copyTo(target, overwrite = true)
                Uri.fromFile(target)
            }
        }
    }

    fun contentUriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun safeFileName(invoiceNumber: String): String {
        val safeNumber = invoiceNumber.replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_', '.', '-').ifBlank { "invoice" }
        return "invoice_$safeNumber.pdf"
    }

    private suspend fun <T> pdfResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.e(TAG, "Unable to generate invoice PDF.", error)
        Result.failure(error)
    }

    companion object {
        const val PDF_MIME_TYPE = "application/pdf"
        private const val TAG = "InvoicePdfGenerator"
    }
}
