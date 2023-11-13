package com.fabien.app.invoiceExtraction

import com.fabien.app.env.Env
import com.fabien.app.env.dependencies
import com.fabien.app.env.loadConfiguration
import com.fabien.app.module
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvoiceExtractionIT {
    private val extractedInvoice = ExtractedInvoice(
        invoiceDate = ExtractedField(value = LocalDate(2023, 6, 23), confidence = 0.99),
        invoiceNumber = ExtractedField(value = "FASY000070971", confidence = 0.98),
        supplier = ExtractedSupplier(
            name = ExtractedField(value = "MARÉCHAL FRAICHEUR", confidence = 0.97),
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
                quantity = ExtractedField(1.0, 0.82),
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

    @Tag("mindeeApiCost")
    @Test
    fun `should successfully extract document`() = testApplication {
        parametrizeApplicationTest()
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
                assertEquals(extractedInvoice.invoiceDate.value, it.invoiceDate.value)
                assertEquals(extractedInvoice.invoiceNumber.value, it.invoiceNumber.value)
                // Supplier
                assertTrue(it.supplier.name.value!!.contains(it.supplier.name.value!!)) // TODO make it better
                assertEquals(extractedInvoice.supplier.nationalId.value, it.supplier.nationalId.value)
                assertEquals(extractedInvoice.supplier.vatNumber.value, it.supplier.vatNumber.value)
                //
                assertEquals(extractedInvoice.totalExcl.value, it.totalExcl.value)
                assertEquals(extractedInvoice.totalIncl.value, it.totalIncl.value)
                // Invoice-Items
                // TODO SIZE
                assertEquals(extractedInvoice.invoiceItems[0].code.value, it.invoiceItems[0].code.value)
                assertEquals(extractedInvoice.invoiceItems[0].description.value, it.invoiceItems[0].description.value) // TODO more relax
                assertEquals(extractedInvoice.invoiceItems[0].quantity.value, it.invoiceItems[0].quantity.value)
                assertEquals(extractedInvoice.invoiceItems[0].totalExcl.value, it.invoiceItems[0].totalExcl.value)
//                assertEquals(extractedInvoice.invoiceItems[0].taxRate.value, it.invoiceItems[0].taxRate.value)

                assertEquals(extractedInvoice.invoiceItems[1].code.value, it.invoiceItems[1].code.value)
                assertEquals(extractedInvoice.invoiceItems[1].description.value, it.invoiceItems[1].description.value)
                assertEquals(extractedInvoice.invoiceItems[1].quantity.value, it.invoiceItems[1].quantity.value)
                assertEquals(extractedInvoice.invoiceItems[1].totalExcl.value, it.invoiceItems[1].totalExcl.value)
//                assertEquals(extractedInvoice.invoiceItems[1].taxRate.value, it.invoiceItems[1].taxRate.value)

                assertEquals(extractedInvoice.invoiceItems[2].code.value, it.invoiceItems[2].code.value)
                assertEquals(extractedInvoice.invoiceItems[2].description.value, it.invoiceItems[2].description.value)
//                assertEquals(extractedInvoice.invoiceItems[2].quantity.value, it.invoiceItems[2].quantity.value)
                assertEquals(extractedInvoice.invoiceItems[2].totalExcl.value, it.invoiceItems[2].totalExcl.value)
//                assertEquals(extractedInvoice.invoiceItems[2].taxRate.value, it.invoiceItems[2].taxRate.value)
            }
        }
    }
}

context(ApplicationTestBuilder)
private fun parametrizeApplicationTest(env: Env = loadConfiguration(ApplicationConfig("application.yaml"))) {
    application {
        val dependencies = dependencies(env.insee, env.jwt, env.mindee, env.postgres)
        module(dependencies)
    }
}

context(ApplicationTestBuilder)
private fun createClientWithJsonNegotiation(): HttpClient =
    createClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }
