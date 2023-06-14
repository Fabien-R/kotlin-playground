package com.fabien.invoiceExtraction

import com.fabien.invoiceExtraction.mindee.MindeeApi
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureExtractionRouting(mindeeApi: MindeeApi) {
    routing {
        post("/extract") {
            call.receiveMultipart().forEachPart {
                if (it is PartData.FileItem) {
                    it.streamProvider().use { input ->
                        mindeeApi.fetchInvoiceExtraction(input).let { extraction ->
                            call.respond(HttpStatusCode.OK, extraction)
                        }
                    }
                }
            }
        }
    }
}
