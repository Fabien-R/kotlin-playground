package com.fabien.domain.services

import arrow.core.Either
import com.fabien.domain.InvoiceExtractionError
import com.fabien.domain.model.ExtractedInvoice
import java.io.InputStream

interface InvoiceExtractionService {
    suspend fun fetchInvoiceExtraction(file: InputStream): Either<InvoiceExtractionError, ExtractedInvoice>
}
