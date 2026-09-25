package com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.freeinvoicegeneratorbydaybookcloud.data.preferences.BusinessSettingsRepository
import com.example.freeinvoicegeneratorbydaybookcloud.data.preferences.InvoiceSettings
import com.example.freeinvoicegeneratorbydaybookcloud.data.preferences.InvoiceSettingsRepository
import com.example.freeinvoicegeneratorbydaybookcloud.data.template.RemoteInvoiceTemplateStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val invoiceSettingsRepository: InvoiceSettingsRepository,
    private val businessSettingsRepository: BusinessSettingsRepository
) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("daybook_settings", Context.MODE_PRIVATE)

    val organizationName = businessSettingsRepository.settings.map { it.name }
        .stateIn(viewModelScope, SharingStarted.Eagerly, businessSettingsRepository.settings.value.name)
    val organizationAddress = businessSettingsRepository.settings.map { it.address }
        .stateIn(viewModelScope, SharingStarted.Eagerly, businessSettingsRepository.settings.value.address)
    val userEmail = businessSettingsRepository.settings.map { it.email }
        .stateIn(viewModelScope, SharingStarted.Eagerly, businessSettingsRepository.settings.value.email)
    val phoneNumber = businessSettingsRepository.settings.map { it.phone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, businessSettingsRepository.settings.value.phone)
    val organizationLogoPath = businessSettingsRepository.settings.map { it.logoPath }
        .stateIn(viewModelScope, SharingStarted.Eagerly, businessSettingsRepository.settings.value.logoPath)

    private val _themeMode = MutableStateFlow(runCatching {
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    }.getOrDefault(ThemeMode.SYSTEM))

    val themeMode = _themeMode.asStateFlow()
    val invoiceSettings: StateFlow<InvoiceSettings> = invoiceSettingsRepository.settings
    internal val remoteTemplates = RemoteInvoiceTemplateStore.templates
    internal val templateCatalogLoaded = RemoteInvoiceTemplateStore.catalogLoaded
    private val _templatePageLoading = MutableStateFlow(false)
    internal val templatePageLoading = _templatePageLoading.asStateFlow()
    private val _templatePageError = MutableStateFlow<String?>(null)
    internal val templatePageError = _templatePageError.asStateFlow()
    private var loadedTemplatePageKey: String? = null
    private var failedTemplatePageKey: String? = null

    init {
        viewModelScope.launch { runCatching { RemoteInvoiceTemplateStore.load() } }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun updateInvoiceSettings(prefix: String, taxRate: String, currencyCode: String) {
        invoiceSettingsRepository.update(
            prefix,
            taxRate.toIntOrNull() ?: invoiceSettings.value.taxRatePercent,
            currencyCode
        )
    }

    fun selectInvoiceTemplate(templateId: String) {
        invoiceSettingsRepository.updateTemplate(templateId)
    }

    internal fun loadTemplatePage(templateIds: List<String>) {
        if (templateIds.isEmpty() || _templatePageLoading.value) return
        val pageKey = templateIds.joinToString("|")
        if (pageKey == loadedTemplatePageKey || pageKey == failedTemplatePageKey) return
        viewModelScope.launch {
            _templatePageLoading.value = true
            _templatePageError.value = null
            RemoteInvoiceTemplateStore.loadHtml(templateIds)
                .onSuccess { loadedTemplatePageKey = pageKey; failedTemplatePageKey = null }
                .onFailure {
                    failedTemplatePageKey = pageKey
                    _templatePageError.value = "Could not load these templates. Check your connection and retry."
                }
            _templatePageLoading.value = false
        }
    }

    internal fun retryTemplatePage(templateIds: List<String>) {
        failedTemplatePageKey = null
        _templatePageError.value = null
        loadTemplatePage(templateIds)
    }

    internal fun isTemplateLoaded(templateId: String): Boolean =
        RemoteInvoiceTemplateStore.htmlFor(templateId) != null

    fun updateOrgName(name: String) {
        val current = businessSettingsRepository.settings.value
        businessSettingsRepository.update(name, current.address, current.email, current.phone)
    }

    fun updateOrganization(name: String, address: String, email: String, phone: String, logoPath: String?) {
        businessSettingsRepository.update(name, address, email, phone, logoPath)
    }
}
