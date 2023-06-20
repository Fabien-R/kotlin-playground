package com.fabien.invoiceExtraction.mindee

import arrow.core.Either
import com.fabien.MindeeIOError
import com.fabien.MindeeOtherError
import com.fabien.MindeeUnAuthorizedError
import com.fabien.invoiceExtraction.*
import com.mindee.MindeeClient
import com.mindee.parsing.common.field.*
import com.mindee.parsing.invoice.InvoiceLineItem
import com.mindee.parsing.invoice.InvoiceV4DocumentPrediction
import com.mindee.parsing.invoice.InvoiceV4Inference
import com.mindee.utils.MindeeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.datetime.toKotlinLocalDate
import java.io.IOException
import java.io.InputStream

enum class MindeeCompanyRegistrationType(val type: String) {
    SIRET("SIRET"),
    SIREN("SIREN"),
    VAT_NUMBER("VAT NUMBER"),
}

fun interface MindeeInvoiceExtractionApi : InvoiceExtractionApi {

    fun InvoiceV4DocumentPrediction.toExtractedInvoice() = ExtractedInvoice(
        invoiceDate = this.invoiceDateField.toExtractedField(),
        invoiceNumber = this.invoiceNumber.toExtractedField(),
        supplier = this.getExtractedSupplier(),
        totalExcl = this.totalNet.toExtractedField(),
        totalIncl = this.totalAmount.toExtractedField(),
        taxes = this.taxes.map { it.toExtractedTax() },
        invoiceItems = this.lineItems.map { it.toExtractedItem() },
    )

    // Not good that mindee does not dissociate confidence for rate & amount
    fun TaxField.toExtractedTax() = ExtractedTax(rate = ExtractedField(this.rate, this.confidence), amount = ExtractedField(this.value, this.confidence))

    // Not good that mindee does not dissociate confidence for each field of an item
    fun InvoiceLineItem.toExtractedItem() = ExtractedItem(
        code = ExtractedField(this.productCode, this.confidence),
        description = ExtractedField(this.description, this.confidence),
        quantity = ExtractedField(this.quantity, this.confidence),
        totalExcl = ExtractedField(this.totalAmount, this.confidence),
        taxRate = ExtractedField(this.taxRate, this.confidence),
    )

    fun InvoiceV4DocumentPrediction.getExtractedSupplier() = ExtractedSupplier(
        name = this.supplierName.toExtractedField(),
        address = this.supplierAddress.toExtractedField(),
        nationalId = this.supplierCompanyRegistrations.toNationalId(),
        vatNumber = this.supplierCompanyRegistrations.firstOrNull { it.type == MindeeCompanyRegistrationType.VAT_NUMBER.type }.toExtractedField(),
    )

    fun List<CompanyRegistrationField>.toNationalId() =
        (
            this.firstOrNull { it.type == MindeeCompanyRegistrationType.SIRET.type && it.value != null }
                ?: this.firstOrNull { it.type == MindeeCompanyRegistrationType.SIREN.type && it.value != null }
            ).toExtractedField()

    private fun AmountField.toExtractedField() = ExtractedField(this.value, this.confidence)
    private fun StringField.toExtractedField() = ExtractedField(this.value, this.confidence)
    private fun DateField.toExtractedField() = ExtractedField(this.value.toKotlinLocalDate(), this.confidence)
    private fun CompanyRegistrationField?.toExtractedField() = ExtractedField(this?.value, this?.confidence ?: 0.0)
}

fun mindeeApi(client: MindeeClient) = object : MindeeInvoiceExtractionApi {
    override suspend fun fetchInvoiceExtraction(file: InputStream) =
        Either.catch {
            runInterruptible(Dispatchers.IO) {
                client.loadDocument(file, "plop").let { document ->
                    client.parse(InvoiceV4Inference::class.java, document).inference.documentPrediction.toExtractedInvoice()
                }
            }
        }.mapLeft { mindeeException ->
            when (mindeeException) {
                is MindeeException -> {
                    when {
                        mindeeException.message?.contains("Unauthorized") ?: false -> MindeeUnAuthorizedError(mindeeException.message ?: "Unknown error")
                        else -> MindeeOtherError(mindeeException.message ?: "Unknown error")
                    }
                }
                is IOException -> {
                    MindeeIOError(mindeeException.message ?: "Unknown error")
                }
                else -> {
                    MindeeOtherError(mindeeException.message ?: "Unknown error")
                }
            }
        }
}
