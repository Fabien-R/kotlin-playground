package com.fabien.invoiceExtraction

import com.fabien.invoiceExtraction.mindee.respond
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureExtractionRouting(invoiceExtractionApi: InvoiceExtractionApi) {
    routing {
        post("/extract") {
            call.receiveMultipart().forEachPart {
                if (it is PartData.FileItem) {
                    it.streamProvider().use { input ->
                        invoiceExtractionApi.fetchInvoiceExtraction(input).respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}