package com.fabien.invoiceExtraction.mindee

import com.fabien.invoiceExtraction.ExtractedInvoice
import com.fabien.invoiceExtraction.ExtractedItem
import com.fabien.invoiceExtraction.ExtractedTax
import com.mindee.MindeeClient
import com.mindee.parsing.common.field.TaxField
import com.mindee.parsing.invoice.InvoiceLineItem
import com.mindee.parsing.invoice.InvoiceV4DocumentPrediction
import com.mindee.parsing.invoice.InvoiceV4Inference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinLocalDate
import java.io.InputStream

enum class MindeeCompanyRegistrationType(val type: String) {
    SIRET("SIRET"),
    SIREN("SIREN"),
    VAT_NUMBER("VAT NUMBER"),
}

class MindeeApi(private val client: MindeeClient) {
    // FIXME Warning Inappropriate blocking method call
    suspend fun fetchInvoiceExtraction(file: InputStream) =
        withContext(Dispatchers.IO) {
            client.loadDocument(file, "plop").let { document ->
                client.parse(InvoiceV4Inference::class.java, document).inference.documentPrediction.toExtractedInvoice()
            }
        }

    fun InvoiceV4DocumentPrediction.toExtractedInvoice() = ExtractedInvoice(
        invoiceDate = this.invoiceDateField.value.toKotlinLocalDate(),
        invoiceNumber = this.invoiceNumber.value,
        supplier = this.getExtractedSuppliers(),
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

    fun InvoiceV4DocumentPrediction.getExtractedSuppliers() = com.fabien.invoiceExtraction.ExtractedSupplier(
        name = this.supplierName.value,
        address = this.supplierAddress.value,
        nationalId = this.supplierCompanyRegistrations.firstOrNull { it.type == MindeeCompanyRegistrationType.SIRET.type }?.value
            ?: this.supplierCompanyRegistrations.firstOrNull { it.type == MindeeCompanyRegistrationType.SIREN.type }?.value,
        vatNumber = this.supplierCompanyRegistrations.firstOrNull { it.type == MindeeCompanyRegistrationType.VAT_NUMBER.type }?.value,
    )
}
