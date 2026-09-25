package com.example.freeinvoicegeneratorbydaybookcloud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.freeinvoicegeneratorbydaybookcloud.data.preferences.BusinessSettingsRepository
import com.example.freeinvoicegeneratorbydaybookcloud.data.preferences.InvoiceSettingsRepository
import com.example.freeinvoicegeneratorbydaybookcloud.data.template.RemoteInvoiceTemplateStore
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.Customer
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.DateFormatOption
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.Invoice
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.InvoiceStatus
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.InvoiceType
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.Organization
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.PaymentDetails
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.PaymentMethod
import com.example.freeinvoicegeneratorbydaybookcloud.domain.model.TaxOption
import com.example.freeinvoicegeneratorbydaybookcloud.domain.repository.CustomerRepository
import com.example.freeinvoicegeneratorbydaybookcloud.domain.repository.InvoiceRepository
import com.example.freeinvoicegeneratorbydaybookcloud.domain.repository.OrganizationRepository
import com.example.freeinvoicegeneratorbydaybookcloud.pdf.InvoicePdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CreateInvoiceViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val organizationRepository: OrganizationRepository,
    private val invoiceSettingsRepository: InvoiceSettingsRepository,
    private val businessSettingsRepository: BusinessSettingsRepository,
    private val invoicePdfGenerator: InvoicePdfGenerator
) : ViewModel() {
    private var currentInvoiceSettings = invoiceSettingsRepository.settings.value
    private var editingInvoice: Invoice? = null

    private val _uiState = MutableStateFlow(newState(InvoiceType.SIMPLE))
    val uiState: StateFlow<CreateInvoiceUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _editingInvoiceId = MutableStateFlow<Long?>(null)
    val editingInvoiceId: StateFlow<Long?> = _editingInvoiceId.asStateFlow()

    private val _pdfActionState = MutableStateFlow(PdfActionState())
    val pdfActionState: StateFlow<PdfActionState> = _pdfActionState.asStateFlow()

    private val _previewEvents = MutableSharedFlow<InvoicePreviewEvent>()
    val previewEvents: SharedFlow<InvoicePreviewEvent> = _previewEvents.asSharedFlow()

    val invoices: StateFlow<List<InvoiceUiModel>> = invoiceRepository.observeInvoices()
        .map { invoices -> invoices.map { it.toUiModel() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedTemplateId: StateFlow<String> = invoiceSettingsRepository.settings
        .map { it.templateId }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            invoiceSettingsRepository.settings.value.templateId
        )

    internal val remoteTemplates = RemoteInvoiceTemplateStore.templates
    internal val templateCatalogLoaded = RemoteInvoiceTemplateStore.catalogLoaded
    private val _templatePageLoading = MutableStateFlow(false)
    internal val templatePageLoading = _templatePageLoading.asStateFlow()
    private val _templatePageError = MutableStateFlow<String?>(null)
    internal val templatePageError = _templatePageError.asStateFlow()
    private var loadedTemplatePageKey: String? = null
    private var failedTemplatePageKey: String? = null

    private var lastSuggestedInvoiceNumber = "${currentInvoiceSettings.prefix}001"

    init {
        viewModelScope.launch { runCatching { RemoteInvoiceTemplateStore.load() } }
        viewModelScope.launch {
            invoices.collect { saved ->
                val next = nextInvoiceNumber(saved)
                _uiState.update { state ->
                    if (state.invoiceNumber == lastSuggestedInvoiceNumber) {
                        state.copy(invoiceNumber = next)
                    } else {
                        state
                    }
                }
                lastSuggestedInvoiceNumber = next
            }
        }
        viewModelScope.launch {
            invoiceSettingsRepository.settings.collect { settings ->
                val previous = currentInvoiceSettings
                currentInvoiceSettings = settings
                if (_editingInvoiceId.value == null) {
                    _uiState.update { state ->
                        state.copy(
                            invoiceNumber = settings.prefix + state.invoiceNumber.removePrefix(previous.prefix),
                            currencyCode = settings.currencyCode,
                            currencySymbol = settings.currency.symbol,
                            taxRatePercent = settings.taxRatePercent
                        )
                    }
                }
                lastSuggestedInvoiceNumber = nextInvoiceNumber(invoices.value)
            }
        }
    }

    fun selectInvoiceType(type: InvoiceType) {
        editingInvoice = null
        _editingInvoiceId.value = null
        _uiState.value = newState(type).copy(invoiceNumber = nextInvoiceNumber(invoices.value))
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

    fun setStep(step: Int) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun updateOrganization(name: String, address: String) {
        _uiState.update { it.copy(organizationName = name, organizationAddress = address) }
    }

    fun updateOrganizationLogo(logoPath: String?) {
        _uiState.update { it.copy(organizationLogoPath = logoPath) }
    }

    fun updateCustomer(name: String, address: String) {
        _uiState.update { it.copy(customerName = name, customerAddress = address) }
    }

    fun updateInvoiceDetails(number: String, date: String, due: String) {
        _uiState.update { it.copy(invoiceNumber = number, invoiceDate = date, dueDate = due) }
    }

    fun updateCurrency(currencyCode: String, currencySymbol: String, decimalPlaces: Int) {
        _uiState.update {
            it.copy(
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                decimalPlaces = decimalPlaces.coerceIn(0, 3)
            )
        }
    }

    fun updateDateFormat(dateFormat: DateFormatOption) {
        _uiState.update { it.copy(dateFormat = dateFormat) }
    }

    fun selectInvoiceTemplate(templateId: String) {
        invoiceSettingsRepository.updateTemplate(templateId)
    }

    fun updateAdvancedOrganization(
        name: String,
        address: String,
        country: String,
        email: String,
        mobile: String,
        gstin: String,
        authorityName: String,
        designation: String,
        logoPath: String?
    ) {
        _uiState.update {
            it.copy(
                organizationName = name,
                organizationAddress = address,
                organizationCountry = country,
                organizationEmail = email,
                organizationMobile = mobile,
                organizationGstin = gstin.uppercase(),
                authorityName = authorityName,
                authorityDesignation = designation,
                organizationLogoPath = logoPath
            )
        }
    }

    fun updateAdvancedCustomer(
        name: String,
        address: String,
        country: String,
        mobile: String,
        email: String,
        gstin: String
    ) {
        _uiState.update {
            it.copy(
                customerName = name,
                customerAddress = address,
                customerCountry = country,
                customerMobile = mobile,
                customerEmail = email,
                customerGstin = gstin.uppercase()
            )
        }
    }

    fun updateConfiguration(
        number: String,
        date: String,
        due: String,
        currencyCode: String,
        currencySymbol: String,
        decimalPlaces: Int,
        deliveryState: String,
        taxOption: TaxOption,
        dateFormat: DateFormatOption,
        showDescription: Boolean,
        showDiscount: Boolean
    ) {
        _uiState.update {
            it.copy(
                invoiceNumber = number,
                invoiceDate = date,
                dueDate = due,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                decimalPlaces = decimalPlaces.coerceIn(0, 3),
                deliveryState = deliveryState,
                taxOption = taxOption,
                dateFormat = dateFormat,
                showItemDescription = showDescription,
                showItemDiscount = showDiscount
            )
        }
    }

    fun updateItemOptions(internationalNumbering: Boolean, roundOffMinor: Long) {
        _uiState.update { it.copy(internationalNumbering = internationalNumbering, roundOffMinor = roundOffMinor) }
    }

    fun updatePaymentAndAdditional(
        method: PaymentMethod,
        accountNumber: String,
        owner: String,
        bank: String,
        upi: String,
        notes: String,
        terms: String
    ) {
        _uiState.update {
            it.copy(
                paymentMethod = method,
                accountNumber = accountNumber,
                accountOwnerName = owner,
                bankName = bank,
                upiId = upi,
                additionalNotes = notes,
                termsAndConditions = terms
            )
        }
    }

    fun updateAdditional(notes: String, terms: String) {
        _uiState.update { it.copy(additionalNotes = notes, termsAndConditions = terms) }
    }

    fun addItem(
        name: String,
        quantity: Int,
        unitPriceMinor: Long,
        description: String = "",
        discountPercent: Double = 0.0,
        taxPercent: Double = _uiState.value.taxRatePercent.toDouble()
    ) {
        val state = _uiState.value
        val item = InvoiceItemUiModel(
            name = name,
            description = description,
            quantity = quantity,
            unitPriceMinor = unitPriceMinor,
            discountPercent = if (state.showItemDiscount) discountPercent else 0.0,
            cgstPercent = if (state.taxOption == TaxOption.CGST_SGST) taxPercent / 2.0 else 0.0,
            sgstPercent = if (state.taxOption == TaxOption.CGST_SGST) taxPercent / 2.0 else 0.0,
            igstPercent = if (state.taxOption == TaxOption.IGST) taxPercent else 0.0
        )
        _uiState.update { it.copy(items = it.items + item.withCalculatedAmounts(state.invoiceType, state.taxOption)) }
    }

    fun updateItem(
        itemId: String,
        name: String,
        quantity: Int,
        unitPriceMinor: Long,
        description: String = "",
        discountPercent: Double = 0.0,
        taxPercent: Double = _uiState.value.taxRatePercent.toDouble()
    ) {
        val state = _uiState.value
        _uiState.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.id != itemId) {
                        item
                    } else {
                        item.copy(
                            name = name,
                            description = description,
                            quantity = quantity,
                            unitPriceMinor = unitPriceMinor,
                            discountPercent = if (state.showItemDiscount) discountPercent else 0.0,
                            cgstPercent = if (state.taxOption == TaxOption.CGST_SGST) taxPercent / 2.0 else 0.0,
                            sgstPercent = if (state.taxOption == TaxOption.CGST_SGST) taxPercent / 2.0 else 0.0,
                            igstPercent = if (state.taxOption == TaxOption.IGST) taxPercent else 0.0
                        ).withCalculatedAmounts(state.invoiceType, state.taxOption)
                    }
                }
            )
        }
    }

    private fun InvoiceItemUiModel.withCalculatedAmounts(
        invoiceType: InvoiceType,
        taxOption: TaxOption
    ): InvoiceItemUiModel {
        val subtotal = unitPriceMinor * quantity
        if (invoiceType == InvoiceType.SIMPLE) {
            return copy(
                lineSubtotalMinor = subtotal,
                discountPercent = 0.0,
                discountMinor = 0L,
                taxableAmountMinor = subtotal,
                cgstPercent = 0.0,
                sgstPercent = 0.0,
                igstPercent = 0.0,
                taxAmountMinor = 0L,
                totalMinor = subtotal
            )
        }

        val discount = percentage(subtotal, discountPercent)
        val taxable = (subtotal - discount).coerceAtLeast(0L)
        val tax = when (taxOption) {
            TaxOption.CGST_SGST -> percentage(taxable, cgstPercent) + percentage(taxable, sgstPercent)
            TaxOption.IGST -> percentage(taxable, igstPercent)
            TaxOption.NON_TAXABLE -> 0L
        }
        return copy(
            lineSubtotalMinor = subtotal,
            discountMinor = discount,
            taxableAmountMinor = taxable,
            taxAmountMinor = tax,
            totalMinor = taxable + tax
        )
    }

    private fun percentage(amountMinor: Long, percent: Double): Long =
        BigDecimal.valueOf(amountMinor)
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100L), 0, RoundingMode.HALF_UP)
            .longValueExact()

    fun removeItem(itemId: String) {
        _uiState.update { it.copy(items = it.items.filterNot { item -> item.id == itemId }) }
    }

    fun startNewInvoice() {
        editingInvoice = null
        _editingInvoiceId.value = null
        _saveError.value = null
        _uiState.value = newState(InvoiceType.SIMPLE).copy(invoiceNumber = nextInvoiceNumber(invoices.value))
    }

    fun beginEditing(invoiceId: Long, onReady: () -> Unit) {
        viewModelScope.launch {
            val invoice = invoiceRepository.getInvoiceById(invoiceId) ?: run {
                _saveError.value = "Invoice not found."
                return@launch
            }
            editingInvoice = invoice
            _editingInvoiceId.value = invoice.id
            _saveError.value = null
            _uiState.value = CreateInvoiceUiState(
                invoiceType = invoice.invoiceType,
                organizationName = invoice.organization?.name.orEmpty(),
                organizationAddress = invoice.organization?.address.orEmpty(),
                organizationCountry = invoice.organization?.country.orEmpty(),
                organizationEmail = invoice.organization?.email.orEmpty(),
                organizationMobile = invoice.organization?.mobile ?: invoice.organization?.phone.orEmpty(),
                organizationGstin = invoice.organization?.gstin ?: invoice.organization?.taxNumber.orEmpty(),
                authorityName = invoice.organization?.authorityName.orEmpty(),
                authorityDesignation = invoice.organization?.authorityDesignation.orEmpty(),
                organizationLogoPath = invoice.organization?.logoPath,
                customerName = invoice.customer?.name.orEmpty(),
                customerAddress = invoice.customer?.address.orEmpty(),
                customerCountry = invoice.customer?.country.orEmpty(),
                customerMobile = invoice.customer?.mobile ?: invoice.customer?.phone.orEmpty(),
                customerEmail = invoice.customer?.email.orEmpty(),
                customerGstin = invoice.customer?.gstin ?: invoice.customer?.taxNumber.orEmpty(),
                invoiceNumber = invoice.invoiceNumber,
                invoiceDate = formatInputDate(invoice.invoiceDate),
                dueDate = formatInputDate(invoice.dueDate),
                currencyCode = invoice.currencyCode,
                currencySymbol = invoice.currencySymbol,
                decimalPlaces = invoice.decimalPlaces,
                deliveryState = invoice.deliveryState.orEmpty(),
                taxOption = invoice.taxOption,
                dateFormat = invoice.dateFormat,
                showItemDescription = invoice.showItemDescription,
                showItemDiscount = invoice.showItemDiscount,
                internationalNumbering = invoice.internationalNumbering,
                roundOffMinor = invoice.roundOffMinor,
                paymentMethod = invoice.paymentDetails?.paymentMethod ?: PaymentMethod.NONE,
                accountNumber = invoice.paymentDetails?.accountNumber.orEmpty(),
                accountOwnerName = invoice.paymentDetails?.accountOwnerName.orEmpty(),
                bankName = invoice.paymentDetails?.bankName.orEmpty(),
                upiId = invoice.paymentDetails?.upiId.orEmpty(),
                additionalNotes = invoice.additionalNotes ?: invoice.notes.orEmpty(),
                termsAndConditions = invoice.termsAndConditions.orEmpty(),
                items = invoice.items.map { it.toUiItem() },
                taxRatePercent = invoice.items.firstOrNull()?.taxPercent?.roundToInt()
                    ?: currentInvoiceSettings.taxRatePercent
            )
            onReady()
        }
    }

    fun deleteInvoice(invoiceId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val invoice = invoiceRepository.getInvoiceById(invoiceId) ?: return@launch
                invoiceRepository.deleteInvoice(invoice)
                if (_editingInvoiceId.value == invoiceId) startNewInvoice()
                onSuccess()
            } catch (error: Exception) {
                _saveError.value = error.message ?: "Could not delete the invoice."
            }
        }
    }

    fun downloadInvoicePdf(invoiceId: Long) {
        if (_pdfActionState.value.isGeneratingPdf) return
        val invoice = invoices.value.firstOrNull { it.id == invoiceId } ?: return
        _pdfActionState.value = PdfActionState(isGeneratingPdf = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                invoicePdfGenerator.saveToDownloads(invoice, selectedTemplateId.value)
            }
            result
                .onSuccess { _previewEvents.emit(InvoicePreviewEvent.DownloadSuccess(it)) }
                .onFailure { _previewEvents.emit(InvoicePreviewEvent.DownloadError) }
            _pdfActionState.value = PdfActionState()
        }
    }

    fun shareInvoicePdf(invoiceId: Long) {
        if (_pdfActionState.value.isGeneratingPdf) return
        val invoice = invoices.value.firstOrNull { it.id == invoiceId } ?: return
        _pdfActionState.value = PdfActionState(isGeneratingPdf = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                invoicePdfGenerator.createSharePdf(invoice, selectedTemplateId.value).map { file ->
                    invoicePdfGenerator.contentUriFor(file) to file.name
                }
            }
            result
                .onSuccess { (uri, fileName) -> _previewEvents.emit(InvoicePreviewEvent.ShareReady(uri, fileName)) }
                .onFailure { _previewEvents.emit(InvoicePreviewEvent.ShareError) }
            _pdfActionState.value = PdfActionState()
        }
    }

    fun createInvoice(onSuccess: (Long) -> Unit) {
        if (_isSaving.value) return
        val state = _uiState.value
        _isSaving.value = true
        _saveError.value = null
        viewModelScope.launch {
            try {
                val existing = editingInvoice
                val organization = Organization(
                    id = existing?.organizationId ?: 0L,
                    name = state.organizationName.trim(),
                    address = state.organizationAddress.trim(),
                    country = state.organizationCountry.trim().ifBlank { null },
                    email = state.organizationEmail.trim().ifBlank { null },
                    phone = state.organizationMobile.trim().ifBlank { null },
                    mobile = state.organizationMobile.trim().ifBlank { null },
                    taxNumber = state.organizationGstin.trim().uppercase().ifBlank { null },
                    gstin = state.organizationGstin.trim().uppercase().ifBlank { null },
                    authorityName = state.authorityName.trim().ifBlank { null },
                    authorityDesignation = state.authorityDesignation.trim().ifBlank { null },
                    logoPath = state.organizationLogoPath,
                    createdAt = existing?.organization?.createdAt ?: System.currentTimeMillis()
                )
                val organizationId = if (existing == null) {
                    organizationRepository.saveOrganization(organization)
                } else {
                    organizationRepository.updateOrganization(organization)
                    organization.id
                }

                val customer = Customer(
                    id = existing?.customerId ?: 0L,
                    name = state.customerName.trim(),
                    address = state.customerAddress.trim(),
                    country = state.customerCountry.trim().ifBlank { null },
                    email = state.customerEmail.trim().ifBlank { null },
                    phone = state.customerMobile.trim().ifBlank { null },
                    mobile = state.customerMobile.trim().ifBlank { null },
                    taxNumber = state.customerGstin.trim().uppercase().ifBlank { null },
                    gstin = state.customerGstin.trim().uppercase().ifBlank { null },
                    createdAt = existing?.customer?.createdAt ?: System.currentTimeMillis()
                )
                val customerId = if (existing == null) {
                    customerRepository.saveCustomer(customer)
                } else {
                    customerRepository.updateCustomer(customer)
                    customer.id
                }

                val calculation = state.calculation
                val invoice = (existing ?: Invoice(
                    invoiceNumber = state.invoiceNumber,
                    organizationId = organizationId,
                    customerId = customerId,
                    invoiceDate = parseDate(state.invoiceDate),
                    dueDate = parseDate(state.dueDate),
                    subtotalMinor = calculation.subtotalMinor,
                    taxAmountMinor = calculation.taxAmountMinor,
                    totalMinor = calculation.totalMinor
                )).copy(
                    invoiceNumber = state.invoiceNumber.trim(),
                    organizationId = organizationId,
                    customerId = customerId,
                    invoiceDate = parseDate(state.invoiceDate),
                    dueDate = parseDate(state.dueDate),
                    subtotalMinor = calculation.subtotalMinor,
                    discountMinor = calculation.discountMinor,
                    taxAmountMinor = calculation.taxAmountMinor,
                    totalMinor = calculation.totalMinor,
                    status = existing?.status ?: InvoiceStatus.DRAFT,
                    notes = state.additionalNotes.ifBlank { null },
                    invoiceType = state.invoiceType,
                    currencyCode = state.currencyCode,
                    currencySymbol = state.currencySymbol,
                    decimalPlaces = state.decimalPlaces,
                    deliveryState = state.deliveryState.ifBlank { null },
                    taxOption = if (state.invoiceType == InvoiceType.SIMPLE) TaxOption.NON_TAXABLE else state.taxOption,
                    dateFormat = state.dateFormat,
                    showItemDescription = state.invoiceType == InvoiceType.ADVANCED && state.showItemDescription,
                    showItemDiscount = state.invoiceType == InvoiceType.ADVANCED && state.showItemDiscount,
                    internationalNumbering = state.internationalNumbering,
                    roundOffMinor = calculation.roundOffMinor,
                    additionalNotes = state.additionalNotes.ifBlank { null },
                    termsAndConditions = state.termsAndConditions.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
                val payment = if (state.invoiceType == InvoiceType.ADVANCED) {
                    PaymentDetails(
                        invoiceId = existing?.id ?: 0L,
                        paymentMethod = state.paymentMethod,
                        accountNumber = state.accountNumber.ifBlank { null },
                        accountOwnerName = state.accountOwnerName.ifBlank { null },
                        bankName = state.bankName.ifBlank { null },
                        upiId = state.upiId.ifBlank { null }
                    )
                } else {
                    null
                }
                val invoiceId = if (existing == null) {
                    invoiceRepository.saveInvoice(invoice, calculation.items, payment)
                } else {
                    invoiceRepository.updateInvoice(invoice, calculation.items, payment)
                    existing.id
                }
                editingInvoice = null
                _editingInvoiceId.value = null
                _uiState.value = newState(InvoiceType.SIMPLE).copy(
                    invoiceNumber = nextInvoiceNumber(invoices.value, invoiceId)
                )
                onSuccess(invoiceId)
            } catch (error: Exception) {
                _saveError.value = error.message ?: "Could not save the invoice. Please try again."
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun newState(type: InvoiceType): CreateInvoiceUiState = CreateInvoiceUiState(
        invoiceType = type,
        organizationName = businessSettingsRepository.settings.value.name,
        organizationAddress = businessSettingsRepository.settings.value.address,
        organizationEmail = businessSettingsRepository.settings.value.email,
        organizationMobile = businessSettingsRepository.settings.value.phone,
        organizationLogoPath = businessSettingsRepository.settings.value.logoPath,
        invoiceNumber = "${currentInvoiceSettings.prefix}001",
        currencyCode = currentInvoiceSettings.currencyCode,
        currencySymbol = currentInvoiceSettings.currency.symbol,
        taxRatePercent = currentInvoiceSettings.taxRatePercent,
        taxOption = if (type == InvoiceType.ADVANCED) TaxOption.CGST_SGST else TaxOption.NON_TAXABLE
    )

    private fun nextInvoiceNumber(invoices: List<InvoiceUiModel>, savedId: Long = 0L): String {
        val nextId = maxOf(invoices.maxOfOrNull { it.id } ?: 0L, savedId) + 1L
        return "${currentInvoiceSettings.prefix}${nextId.toString().padStart(3, '0')}"
    }

}
