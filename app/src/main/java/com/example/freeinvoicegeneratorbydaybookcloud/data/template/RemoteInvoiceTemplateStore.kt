package com.example.freeinvoicegeneratorbydaybookcloud.data.template

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

internal data class RemoteInvoiceTemplate(
    val id: String,
    val name: String,
    val description: String,
    val path: String,
    val tags: List<String>
)

internal object RemoteInvoiceTemplateStore {
    private const val CATALOG_URL = "https://free-invoice-generator-dev.daybook.cloud/invoice-templates/templates.json"
    private const val HTML_ROOT = "https://free-invoice-generator-dev.daybook.cloud/"

    private val _templates = MutableStateFlow<List<RemoteInvoiceTemplate>>(emptyList())
    val templates: StateFlow<List<RemoteInvoiceTemplate>> = _templates.asStateFlow()
    private val _catalogLoaded = MutableStateFlow(false)
    val catalogLoaded: StateFlow<Boolean> = _catalogLoaded.asStateFlow()
    private val htmlById = ConcurrentHashMap<String, String>()
    private val loadMutex = Mutex()
    private var loaded = false

    suspend fun load() = loadMutex.withLock {
        withContext(Dispatchers.IO) {
            if (loaded) return@withContext
            val catalog = read(CATALOG_URL)
            val parsed = parseCatalog(catalog)
            _templates.value = parsed
            _catalogLoaded.value = true
            loaded = true
        }
    }

    suspend fun loadHtml(ids: Collection<String>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val requested = ids.mapNotNull { id ->
                _templates.value.firstOrNull { it.id == canonicalId(id) }
            }.filterNot { htmlById.containsKey(it.id) }
            requested.chunked(6).forEach { batch ->
                coroutineScope {
                    batch.map { template ->
                        async {
                            htmlById[template.id] = read(HTML_ROOT + template.path)
                        }
                    }.awaitAll()
                }
            }
        }
    }

    fun htmlFor(id: String): String? = htmlById[canonicalId(id)]

    fun pathFor(id: String): String? = _templates.value.firstOrNull { it.id == canonicalId(id) }?.path

    private fun canonicalId(id: String): String = when (id) {
        "elegant_blue" -> "blue_split"
        "royal_purple" -> "royal_plum"
        else -> id
    }

    private fun parseCatalog(json: String): List<RemoteInvoiceTemplate> {
        val groups = JSONArray(json)
        return buildList {
            for (groupIndex in 0 until groups.length()) {
                val items = groups.getJSONObject(groupIndex).optJSONArray("items") ?: continue
                for (itemIndex in 0 until items.length()) {
                    val item = items.getJSONObject(itemIndex)
                    val path = item.optString("path")
                    if (!path.startsWith("invoice-templates/") || !path.endsWith(".html")) continue
                    val id = curatedAliasForPath(path) ?: path.removePrefix("invoice-templates/")
                        .removeSuffix(".html")
                        .replace(Regex("[^A-Za-z0-9]+"), "_")
                        .trim('_')
                        .lowercase()
                    val tagsJson = item.optJSONArray("tags")
                    val tags = buildList {
                        if (tagsJson != null) for (tagIndex in 0 until tagsJson.length()) add(tagsJson.optString(tagIndex))
                    }
                    add(RemoteInvoiceTemplate(id, item.optString("name", id), item.optString("description"), path, tags))
                }
            }
        }.distinctBy { it.id }
    }

    private fun curatedAliasForPath(path: String): String? = mapOf(
        "invoice-templates/modern/teal-mark-invoice-template.html" to "modern_teal",
        "invoice-templates/simple-design/coral-breeze.html" to "coral_breeze",
        "invoice-templates/simple-design/crimson-edge.html" to "crimson_edge",
        "invoice-templates/simple-design/ruby-luxe.html" to "ruby_luxe",
        "invoice-templates/simple-design/violet-gradient.html" to "violet_gradient",
        "invoice-templates/simple-design/watercolor-gradient.html" to "watercolor_gradient",
        "invoice-templates/startup/blue-split-invoice-template.html" to "blue_split",
        "invoice-templates/startup/inferno-line-invoice-template.html" to "inferno_line",
        "invoice-templates/startup/poppins-breeze-invoice-template.html" to "poppins_breeze",
        "invoice-templates/tech-service/bluecrest-invoice-template.html" to "bluecrest",
        "invoice-templates/tech-service/elegant-gold-invoice-template.html" to "elegant_gold",
        "invoice-templates/tech-service/modern-circle-invoice-template.html" to "modern_circle",
        "invoice-templates/tech-service/royal-plum-invoice-template.html" to "royal_plum",
        "invoice-templates/tech-service/teal-flow-invoice-template.html" to "teal_flow",
        "invoice-templates/elegant/indigo-mark-invoice-template.html" to "center_mark",
        "invoice-templates/simple-design/sidebar-slate-invoice-template.html" to "left_rail",
        "invoice-templates/modern/amber-stripe-invoice-template.html" to "stripe_classic",
        "invoice-templates/retail/sunflare-invoice-template.html" to "total_focus",
        "invoice-templates/buissness/amber-edge-invoice-template.html" to "boxed_meta",
        "invoice-templates/simple-design/classic-minimal.html" to "minimal_letter",
        "invoice-templates/freelancer/vertical-split.html" to "split_brand",
        "invoice-templates/corporate/navy-ledger-invoice-template.html" to "ledger_pro",
        "invoice-templates/creative/studio-luxe-invoice-template.html" to "studio_card",
        "invoice-templates/corporate/charcoal-line-invoice-template.html" to "corporate_panel",
        "invoice-templates/simple-design/contemporary-business.html" to "classic_business",
        "invoice-templates/freelancer/minimalist.html" to "minimal_black",
        "invoice-templates/freelancer/modern-green.html" to "soft_green",
        "invoice-templates/freelancer/luxury-gold.html" to "premium_gold",
        "invoice-templates/elegant/slate-edge-invoice-template.html" to "corporate_slate",
        "invoice-templates/freelancer/creative-wave.html" to "creative_coral",
        "invoice-templates/elegant/blue-ledger-invoice-template.html" to "clean_ledger"
    )[path]

    private fun read(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json, text/html")
        return connection.inputStream.bufferedReader().use { it.readText() }.also { connection.disconnect() }
    }
}
