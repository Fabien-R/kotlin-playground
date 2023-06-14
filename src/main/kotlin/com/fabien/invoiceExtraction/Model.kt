package com.fabien.invoiceExtraction

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// Add confidence to each field
@Serializable
data class ExtractedInvoice(
    val invoiceDate: LocalDate?,
    val invoiceNumber: String?,
    val supplier: ExtractedSupplier,
    val totalExcl: Double?,
    val totalIncl: Double?,
    val taxes: List<ExtractedTax>,
    val invoiceItems: List<ExtractedItem>,
)

@Serializable
data class ExtractedSupplier(
    val name: String?,
    val address: String?,
    val nationalId: String?,
    val vatNumber: String?,
)

@Serializable
data class ExtractedItem(
    val code: String?,
    val description: String?,
    val quantity: Double?,
    val totalExcl: Double?,
    val taxRate: Double?,
    // val confidence: Double,
)

@Serializable
data class ExtractedTax(
    val rate: Double?,
    val amount: Double?,
)
