package com.fabien.invoiceExtraction

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.io.InputStream

@Serializable
data class ExtractedField<T>(
    val value: T?,
    val confidence: Double,
)

// Add confidence to each field
@Serializable
data class ExtractedInvoice(
    val invoiceDate: ExtractedField<LocalDate>,
    val invoiceNumber: ExtractedField<String>,
    val supplier: ExtractedSupplier,
    val totalExcl: ExtractedField<Double>,
    val totalIncl: ExtractedField<Double>,
    val taxes: List<ExtractedTax>,
    val invoiceItems: List<ExtractedItem>,
)

@Serializable
data class ExtractedSupplier(
    val name: ExtractedField<String>,
    val address: ExtractedField<String>,
    val nationalId: ExtractedField<String>,
    val vatNumber: ExtractedField<String>,
)

@Serializable
data class ExtractedItem(
    val code: ExtractedField<String>,
    val description: ExtractedField<String>,
    val quantity: ExtractedField<Double>,
    val totalExcl: ExtractedField<Double>,
    val taxRate: ExtractedField<Double>,
)

@Serializable
data class ExtractedTax(
    val rate: ExtractedField<Double>,
    val amount: ExtractedField<Double>,
)

interface InvoiceExtractionApi {
    suspend fun fetchInvoiceExtraction(file: InputStream): ExtractedInvoice
}
