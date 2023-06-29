package com.fabien.invoiceExtraction

import arrow.core.Either
import com.fabien.InvoiceExtractionError
import com.fabien.MindeeOtherError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import java.io.InputStream
import kotlin.test.assertEquals

class InvoiceExtractionTest {

    private val extractedInvoice = ExtractedInvoice(
        invoiceDate = ExtractedField(value = LocalDate(2023, 6, 23), confidence = 0.99),
        invoiceNumber = ExtractedField(value = "FASY000070971", confidence = 0.98),
        supplier = ExtractedSupplier(
            name = ExtractedField(value = "SARL LES JARDINS DU PRINTEMPS", confidence = 0.97),
            address = ExtractedField(value = "Chemin de Rosarge VANCIA - 69140 Rillieux la Pape", confidence = 0.96),
            nationalId = ExtractedField(value = "43220462600027", confidence = 0.95),
            vatNumber = ExtractedField(value = "FR49432204626", confidence = 0.94),
        ),
        totalExcl = ExtractedField(value = 28.63, confidence = 0.93),
        totalIncl = ExtractedField(value = 30.20, confidence = 0.92),
        taxes = listOf(
            ExtractedTax(
                rate = ExtractedField(5.5, 0.91),
                amount = ExtractedField(1.57, 0.9),
            ),
        ),
        invoiceItems = listOf(
            ExtractedItem(
                code = ExtractedField("30014", 0.89),
                description = ExtractedField("Panier MAXI Fruits & Légumes 2", 0.88),
                quantity = ExtractedField(1.0, 0.87),
                totalExcl = ExtractedField(24.55, 0.86),
                taxRate = ExtractedField(5.5, 0.85),
            ),
            ExtractedItem(
                code = ExtractedField("08301", 0.84),
                description = ExtractedField("Botte de carotte fane", 0.83),
                quantity = ExtractedField(2.0, 0.82),
                totalExcl = ExtractedField(2.75, 0.81),
                taxRate = ExtractedField(5.5, 0.8),
            ),
            ExtractedItem(
                code = ExtractedField("09708", 0.79),
                description = ExtractedField("Salade Feuille de Chêne Brune", 0.78),
                quantity = ExtractedField(1.0, 0.77),
                totalExcl = ExtractedField(1.33, 0.76),
                taxRate = ExtractedField(5.5, 0.75),
            ),
        ),
    )

    private val successfulInvoiceExtractionApi = object : InvoiceExtractionApi {
        override suspend fun fetchInvoiceExtraction(file: InputStream): Either<InvoiceExtractionError, ExtractedInvoice> {
            return Either.Right(
                extractedInvoice,
            )
        }
    }

    private val errorInvoiceExtractionApi = object : InvoiceExtractionApi {
        override suspend fun fetchInvoiceExtraction(file: InputStream): Either<InvoiceExtractionError, ExtractedInvoice> {
            return Either.Left(
                MindeeOtherError("Simulated error"),
            )
        }
    }

    @Test
    fun `should successfully extract document`() = testApplication {
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
            configureExtractionRouting(successfulInvoiceExtractionApi)
        }
        createClientWithJsonNegotiation().post("extract") {
            javaClass.classLoader.getResource("2023_06_marechal.pdf")?.let {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "data",
                                it.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.Pdf)
                                    append(HttpHeaders.ContentDisposition, "filename=2023_06_marechal.pdf")
                                },
                            )
                        },
                    ),
                )
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            body<ExtractedInvoice>().let {
                assertEquals(extractedInvoice, it)
            }
        }
    }

    @Test
    fun `should fail to extract document`() = testApplication {
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
            configureExtractionRouting(errorInvoiceExtractionApi)
        }
        createClientWithJsonNegotiation().post("extract") {
            javaClass.classLoader.getResource("2023_06_marechal.pdf")?.let {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "data",
                                it.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.Pdf)
                                    append(HttpHeaders.ContentDisposition, "filename=2023_06_marechal.pdf")
                                },
                            )
                        },
                    ),
                )
            }
        }.apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertEquals("Our supplier is not able to extract the document", bodyAsText())
        }
    }
}

context(ApplicationTestBuilder)
private fun createClientWithJsonNegotiation(): HttpClient =
    createClient {
        install(ContentNegotiation) {
            json()
        }
    }
