package com.fabien.invoiceExtraction.mindee

import com.mindee.MindeeClient
import com.mindee.parsing.invoice.InvoiceV4Inference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class MindeeApi(private val client: MindeeClient) {
    // FIXME Warning Inappropriate blocking method call
    suspend fun fetchInvoiceExtraction(file: InputStream) =
        withContext(Dispatchers.IO) {
            client.loadDocument(file, "plop").let { document ->
                client.parse(InvoiceV4Inference::class.java, document).inference.documentPrediction != null
            }
        }
}
