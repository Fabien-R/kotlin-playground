package com.fabien.invoiceExtraction.mindee

import com.fabien.invoiceExtraction.ExtractedInvoice
import com.fabien.invoiceExtraction.ExtractedItem
import com.fabien.invoiceExtraction.ExtractedSupplier
import com.fabien.invoiceExtraction.ExtractedTax
import com.mindee.MindeeClient
import com.mindee.parsing.common.field.CompanyRegistrationField
import com.mindee.parsing.common.field.TaxField
import com.mindee.parsing.invoice.InvoiceLineItem
import com.mindee.parsing.invoice.InvoiceV4DocumentPrediction
import com.mindee.parsing.invoice.InvoiceV4Inference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.datetime.toKotlinLocalDate
import java.io.InputStream

enum class MindeeCompanyRegistrationType(val type: String) {
    SIRET("SIRET"),
    SIREN("SIREN"),
    VAT_NUMBER("VAT NUMBER"),
}

class MindeeApi(private val client: MindeeClient) {
    suspend fun fetchInvoiceExtraction(file: InputStream) =
        runInterruptible(Dispatchers.IO) {
            client.loadDocument(file, "plop").let { document ->
                client.parse(InvoiceV4Inference::class.java, document).inference.documentPrediction.toExtractedInvoice()
            }
        }

    fun InvoiceV4DocumentPrediction.toExtractedInvoice() = ExtractedInvoice(
        invoiceDate = this.invoiceDateField.value.toKotlinLocalDate(),
        invoiceNumber = this.invoiceNumber.value,
        supplier = this.getExtractedSupplier(),
        totalExcl = this.totalNet.value,
        totalIncl = this.totalAmount.value,
        taxes = this.taxes.map { it.toExtractedTax() },
        invoiceItems = this.lineItems.map { it.toExtractedItem() },
    )

    fun TaxField.toExtractedTax() = ExtractedTax(rate = this.rate, amount = this.value)

    fun InvoiceLineItem.toExtractedItem() = ExtractedItem(
        code = this.productCode,
        description = this.description,
        quantity = this.quantity,
        totalExcl = this.totalAmount,
        taxRate = this.taxRate,
    )

    fun InvoiceV4DocumentPrediction.getExtractedSupplier() = ExtractedSupplier(
        name = this.supplierName.value,
        address = this.supplierAddress.value,
        nationalId = this.supplierCompanyRegistrations.toNationalId(),
        vatNumber = this.supplierCompanyRegistrations.firstOrNull { it.type == MindeeCompanyRegistrationType.VAT_NUMBER.type }?.value,
    )

    fun List<CompanyRegistrationField>.toNationalId() =
        this.firstOrNull { it.type == MindeeCompanyRegistrationType.SIRET.type }?.value
            ?: this.firstOrNull { it.type == MindeeCompanyRegistrationType.SIREN.type }?.value
}
