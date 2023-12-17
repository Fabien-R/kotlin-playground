package com.fabien.http.services.mindee

import arrow.core.Either
import com.fabien.domain.*
import com.fabien.domain.model.*
import com.mindee.MindeeClient
import com.mindee.MindeeException
import com.mindee.input.LocalInputSource
import com.mindee.parsing.standard.CompanyRegistrationField
import com.mindee.parsing.standard.TaxField
import com.mindee.product.invoice.InvoiceV4
import com.mindee.product.invoice.InvoiceV4Document
import com.mindee.product.invoice.InvoiceV4LineItem
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.toKotlinLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate

data class ExtractedStringTest(val value: String?, val confidence: Double)
data class ExtractedDateTest(val value: LocalDate?, val confidence: Double)
data class ExtractedDoubleTest(val value: Double?, val confidence: Double)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
internal class MindeeApiTest {

    private val mindeeApiTest: MindeeInvoiceExtractionApi = MindeeInvoiceExtractionApi { mockk() }

    private fun taxes(): List<Arguments> {
        val confidence = 0.67
        return listOf(
            // TaxField, ExtractedTax
            Arguments.of(createTaxField(0.055, 25.01, confidence), ExtractedTax(ExtractedField(0.055, confidence), ExtractedField(25.01, confidence))),
            Arguments.of(createTaxField(null, 25.01, confidence), ExtractedTax(ExtractedField(null, confidence), ExtractedField(25.01, confidence))),
            Arguments.of(createTaxField(0.20, null, confidence), ExtractedTax(ExtractedField(0.20, confidence), ExtractedField(null, confidence))),
            Arguments.of(createTaxField(null, null, confidence), ExtractedTax(ExtractedField(null, confidence), ExtractedField(null, confidence))),
        )
    }

    @ParameterizedTest
    @MethodSource("taxes")
    fun `should extract tax from mindee response`(taxField: TaxField, expected: ExtractedTax) {
        with(mindeeApiTest) {
            assertEquals(expected, taxField.toExtractedTax())
        }
    }

    private fun nationalIds(): List<Arguments> {
        val siren = "SIREN"
        val siret = "SIRET"
        val sirenRegistration = createCompanyRegistrationField(siren, MindeeCompanyRegistrationType.SIREN.type, 0.81)
        val sirenRegistrationWithoutValue = createCompanyRegistrationField(null, MindeeCompanyRegistrationType.SIREN.type, 0.0)
        val siretRegistration = createCompanyRegistrationField(siret, MindeeCompanyRegistrationType.SIRET.type, 0.92)
        val siretRegistrationWithoutValue = createCompanyRegistrationField(null, MindeeCompanyRegistrationType.SIRET.type, 0.0)
        return listOf(
            // list of registrations, result
            Arguments.of(emptyList<CompanyRegistrationField>(), ExtractedField(null, 0.0)),
            Arguments.of(listOf(sirenRegistrationWithoutValue, siretRegistrationWithoutValue), ExtractedField(null, 0.0)),
            Arguments.of(listOf(sirenRegistration), ExtractedField(siren, sirenRegistration.confidence)),
            Arguments.of(listOf(sirenRegistration, siretRegistrationWithoutValue), ExtractedField(siren, sirenRegistration.confidence)),
            Arguments.of(listOf(siretRegistration), ExtractedField(siret, siretRegistration.confidence)),
            Arguments.of(listOf(sirenRegistrationWithoutValue, siretRegistration), ExtractedField(siret, siretRegistration.confidence)),
        )
    }

    @ParameterizedTest
    @MethodSource("nationalIds")
    fun `should compute national id with siret higher order`(registrations: List<CompanyRegistrationField>, expected: ExtractedField<String>) {
        with(mindeeApiTest) {
            assertEquals(expected, registrations.toNationalId())
        }
    }

    private fun suppliers(): List<Arguments> {
        val siret = createCompanyRegistrationField("SIRET", MindeeCompanyRegistrationType.SIRET.type, 0.92)
        val siren = createCompanyRegistrationField("SIREN", MindeeCompanyRegistrationType.SIREN.type, 0.81)
        val vatNumber = createCompanyRegistrationField("VAT_NUMBER", MindeeCompanyRegistrationType.VAT_NUMBER.type, 0.77)
        val name = "BurgerKing"
        val nameConfidence = 0.54
        val address = "1 rue de la frite"
        val addressConfidence = 0.63

        return listOf(
            // name, address, registration, result
            Arguments.of(
                ExtractedStringTest(name, nameConfidence),
                ExtractedStringTest(address, addressConfidence),
                listOf(siren, siret, vatNumber),
                ExtractedSupplier(
                    ExtractedField(name, nameConfidence),
                    ExtractedField(address, addressConfidence),
                    ExtractedField(siret.value, siret.confidence),
                    ExtractedField(vatNumber.value, vatNumber.confidence),
                ),
            ),
            Arguments.of(
                ExtractedStringTest(name, nameConfidence),
                ExtractedStringTest(address, addressConfidence),
                listOf(siren),
                ExtractedSupplier(
                    ExtractedField(name, nameConfidence),
                    ExtractedField(address, addressConfidence),
                    ExtractedField(siren.value, siren.confidence),
                    ExtractedField(null, 0.0),
                ),
            ),
            Arguments.of(
                ExtractedStringTest(name, nameConfidence),
                ExtractedStringTest(address, addressConfidence),
                listOf(vatNumber),
                ExtractedSupplier(
                    ExtractedField(name, nameConfidence),
                    ExtractedField(address, addressConfidence),
                    ExtractedField(null, 0.0),
                    ExtractedField(vatNumber.value, vatNumber.confidence),
                ),
            ),
            Arguments.of(
                ExtractedStringTest(null, 0.0),
                ExtractedStringTest(null, 0.0),
                emptyList<CompanyRegistrationField>(),
                ExtractedSupplier(
                    ExtractedField(null, 0.0),
                    ExtractedField(null, 0.0),
                    ExtractedField(null, 0.0),
                    ExtractedField(null, 0.0),
                ),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("suppliers")
    fun `should extract supplier from mindee response`(
        name: ExtractedStringTest,
        address: ExtractedStringTest,
        registrations: List<CompanyRegistrationField>,
        expected: ExtractedSupplier,
    ) {
        val invoiceDocumentPrediction = createInvoiceDocumentPredictionForSupplier(name, address, registrations)
        with(mindeeApiTest) {
            assertEquals(expected, invoiceDocumentPrediction.getExtractedSupplier())
        }
    }

    private fun items(): List<Arguments> {
        val code = "9999"
        val description = "French Fries"
        val quantity = 3.555
        val totalExcl = 1.98
        val taxRate = 0.055
        val confidence = 0.87
        return listOf(
            // code, description, quantity, totalExcl, taxRate, result
            Arguments.of(
                code,
                description,
                quantity,
                totalExcl,
                taxRate,
                confidence,
                ExtractedItem(
                    ExtractedField(code, confidence),
                    ExtractedField(description, confidence),
                    ExtractedField(quantity, confidence),
                    ExtractedField(totalExcl, confidence),
                    ExtractedField(taxRate, confidence),
                ),
            ),
            Arguments.of(
                null,
                null,
                null,
                null,
                null,
                0.0,
                ExtractedItem(
                    ExtractedField(null, 0.0),
                    ExtractedField(null, 0.0),
                    ExtractedField(null, 0.0),
                    ExtractedField(null, 0.0),
                    ExtractedField(null, 0.0),
                ),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("items")
    fun `should extract item from mindee response`(
        code: String?,
        description: String?,
        quantity: Double?,
        totalExcl: Double?,
        taxRate: Double?,
        confidence: Double,
        expected: ExtractedItem,
    ) {
        val invoiceLineItem = createItem(code, description, quantity, totalExcl, taxRate, confidence)
        with(mindeeApiTest) {
            assertEquals(expected, invoiceLineItem.toExtractedItem())
        }
    }

    private fun invoices(): List<Arguments> {
        val item1 = ExtractedItem(
            ExtractedField("9999", 0.90),
            ExtractedField("French Fries", 0.91),
            ExtractedField(3.555, 0.92),
            ExtractedField(1.98, 0.93),
            ExtractedField(0.055, 0.94),
        )
        val item2 = ExtractedItem(
            ExtractedField(null, 0.0),
            ExtractedField("Nuggets", 0.81),
            ExtractedField(18.0, 0.82),
            ExtractedField(null, 0.0),
            ExtractedField(0.10, 0.94),
        )
        val supplier = ExtractedSupplier(
            ExtractedField("BurgerKing", 0.7),
            ExtractedField("1 rue de la frite", 0.71),
            ExtractedField("SIRET", 0.72),
            ExtractedField("VAT_NUMBER", 0.73),
        )
        val date = ExtractedDateTest(LocalDate.of(2023, 1, 18), 0.4)
        val invoiceNumber = ExtractedStringTest("123456789", 0.3)
        val totalExcl = ExtractedDoubleTest(18.98, 0.2)
        val totalIncl = ExtractedDoubleTest(21.89, 0.1)
        val tax1 = ExtractedTax(rate = ExtractedField(0.055, 0.6), amount = ExtractedField(0.11, 0.61))
        val tax2 = ExtractedTax(rate = ExtractedField(0.10, 0.5), ExtractedField(null, 0.0))

        return listOf(
            // date, number, supplier, totalExcl, totalIncl, taxes, items, result
            Arguments.of(
                date,
                invoiceNumber,
                supplier,
                totalExcl,
                totalIncl,
                listOf(tax1, tax2),
                listOf(item1, item2),
                ExtractedInvoice(
                    ExtractedField(date.value?.toKotlinLocalDate(), date.confidence),
                    ExtractedField(invoiceNumber.value, invoiceNumber.confidence),
                    supplier,
                    ExtractedField(totalExcl.value, totalExcl.confidence),
                    ExtractedField(totalIncl.value, totalIncl.confidence),
                    listOf(tax1, tax2),
                    listOf(item1, item2),
                ),

            ),
        )
    }

    @ParameterizedTest
    @MethodSource("invoices")
    fun `should extract invoice from mindee response`(
        date: ExtractedDateTest,
        number: ExtractedStringTest,
        supplier: ExtractedSupplier,
        totalExcl: ExtractedDoubleTest,
        totalIncl: ExtractedDoubleTest,
        taxes: List<ExtractedTax>,
        items: List<ExtractedItem>,
        expected: ExtractedInvoice,
    ) {
        // https://github.com/mockk/mockk/issues/1033
        val trick = object : MindeeInvoiceExtractionApi {
            override suspend fun fetchInvoiceExtraction(file: InputStream): Either<InvoiceExtractionError, ExtractedInvoice> = mockk()
        }
        with(spyk(trick)) {
            val invoiceDocumentPrediction = createInvoiceDocumentPredictionForInvoice(date, number, supplier, totalExcl, totalIncl, taxes, items)

            val result = invoiceDocumentPrediction.toExtractedInvoice()
            verify {
                invoiceDocumentPrediction.getExtractedSupplier()
            }
            assertEquals(expected, result)
        }
    }

    @Test
    fun `should call mindeeClient when fetching extraction`() = runTest {
        // Given a mocked mindeeClient with its chained calls
        val mindeeClient = mockk<MindeeClient>()
        val fileInputStream = mockk<FileInputStream>()
        val invoicePrediction = mockk<InvoiceV4Document>()
        val fileName = "plop"

        val file = slot<LocalInputSource>()

        // FIXME should mock LocalInputSource constructor to no depend on implementation....
        every {
            fileInputStream.read(any<ByteArray>())
        } returns -1

        with(spyk(mindeeApi(mindeeClient) as MindeeInvoiceExtractionApi)) {
            every {
                mindeeClient.parse(eq(InvoiceV4::class.java), capture(file)).document.inference.prediction
            } returns invoicePrediction

            every {
                invoicePrediction.toExtractedInvoice()
            } returns mockk<ExtractedInvoice>()

            // when fetching invoice extraction
            fetchInvoiceExtraction(fileInputStream)
            // Then mindee client should call its chained calls
            verify {
                mindeeClient.parse(eq(InvoiceV4::class.java), any<LocalInputSource>()).document.inference.prediction
                invoicePrediction.toExtractedInvoice()
            }
            assertEquals(fileName, file.captured.filename)
        }
    }

    private fun mindeeExceptions(): List<Arguments> {
        val unauthorizedMessage = "Unauthorized Access"
        val otherMessage = "Other Error"
        val unknownError = "Unknown error"
        return listOf(
            // mindeeException, expected error
            Arguments.of(MindeeException(unauthorizedMessage), MindeeUnAuthorizedError(unauthorizedMessage)), //
            Arguments.of(MindeeException(otherMessage), MindeeOtherError(otherMessage)), //
            Arguments.of(MindeeException(null), MindeeOtherError(unknownError)), //
            Arguments.of(IOException(otherMessage), MindeeIOError(otherMessage)), //
            Arguments.of(IOException(), MindeeIOError(unknownError)), //
            Arguments.of(IllegalArgumentException(otherMessage), MindeeOtherError(otherMessage)), //
            Arguments.of(IllegalArgumentException(), MindeeOtherError(unknownError)), //
        )
    }

    @ParameterizedTest
    @MethodSource("mindeeExceptions")
    fun `should map MindeeException to domain error`(clientException: Exception, expectedError: MindeeError) = runTest {
        // Given a mocked mindeeClient that generates exception
        val mindeeClient = mockk<MindeeClient>()
        val fileInputStream = mockk<FileInputStream>()

        // FIXME should mock LocalInputSource constructor to no depend on implementation....
        every {
            fileInputStream.read(any<ByteArray>())
        } returns -1

        every {
            mindeeClient.parse(eq(InvoiceV4::class.java), any<LocalInputSource>())
        } throws clientException

        with(mindeeApi(mindeeClient)) {
            // when fetching invoice extraction
            with(fetchInvoiceExtraction(fileInputStream)) {
                // the exception has been translated into extraction domain error
                assertEquals(expectedError, this.leftOrNull())
            }
        }
    }

    private fun createTaxField(rate: Double?, amount: Double?, confidence: Double): TaxField {
        val taxField = mockk<TaxField>()
        every {
            taxField.rate
        } returns rate

        every {
            taxField.value
        } returns amount

        every {
            taxField.confidence
        } returns confidence

        return taxField
    }

    private fun createInvoiceDocumentPredictionForSupplier(
        name: ExtractedStringTest,
        address: ExtractedStringTest,
        registrations: List<CompanyRegistrationField>,
    ): InvoiceV4Document {
        val invoiceDocumentPrediction = mockk<InvoiceV4Document>()
        every {
            invoiceDocumentPrediction.supplierName.value
        } returns name.value

        every {
            invoiceDocumentPrediction.supplierName.confidence
        } returns name.confidence

        every {
            invoiceDocumentPrediction.supplierAddress.value
        } returns address.value

        every {
            invoiceDocumentPrediction.supplierAddress.confidence
        } returns address.confidence

        every {
            invoiceDocumentPrediction.supplierCompanyRegistrations
        } returns registrations

        return invoiceDocumentPrediction
    }

    private fun createCompanyRegistrationField(value: String?, type: String?, confidence: Double): CompanyRegistrationField {
        val companyRegistrationField = mockk<CompanyRegistrationField>()
        every {
            companyRegistrationField.value
        } returns value

        every {
            companyRegistrationField.type
        } returns type

        every {
            companyRegistrationField.confidence
        } returns confidence

        return companyRegistrationField
    }

    private fun createItem(code: String?, description: String?, quantity: Double?, totalExcl: Double?, taxRate: Double?, confidence: Double): InvoiceV4LineItem {
        val item = mockk<InvoiceV4LineItem>()
        every {
            item.productCode
        } returns code

        every {
            item.description
        } returns description

        every {
            item.quantity
        } returns quantity

        every {
            item.totalAmount
        } returns totalExcl

        every {
            item.taxRate
        } returns taxRate

        every {
            item.confidence
        } returns confidence

        return item
    }

    context(MindeeInvoiceExtractionApi)
    private fun createInvoiceDocumentPredictionForInvoice(
        date: ExtractedDateTest,
        number: ExtractedStringTest,
        supplier: ExtractedSupplier,
        totalExcl: ExtractedDoubleTest,
        totalIncl: ExtractedDoubleTest,
        taxes: List<ExtractedTax>,
        items: List<ExtractedItem>,
    ): InvoiceV4Document {
        val invoiceDocumentPrediction = mockk<InvoiceV4Document>()
        val taxesPrediction = mockk<TaxField>()
        val itemsPrediction = mockk<InvoiceV4LineItem>()

        every {
            invoiceDocumentPrediction.invoiceDateField.value
        } returns date.value

        every {
            invoiceDocumentPrediction.invoiceDateField.confidence
        } returns date.confidence

        every {
            invoiceDocumentPrediction.invoiceNumber.value
        } returns number.value

        every {
            invoiceDocumentPrediction.invoiceNumber.confidence
        } returns number.confidence

        every {
            invoiceDocumentPrediction.getExtractedSupplier()
        } answers { supplier }

        every {
            invoiceDocumentPrediction.totalNet.value
        } returns totalExcl.value

        every {
            invoiceDocumentPrediction.totalNet.confidence
        } returns totalExcl.confidence

        every {
            invoiceDocumentPrediction.totalAmount.value
        } returns totalIncl.value

        every {
            invoiceDocumentPrediction.totalAmount.confidence
        } returns totalIncl.confidence

        every {
            taxesPrediction.toExtractedTax()
        } returnsMany taxes

        every {
            invoiceDocumentPrediction.taxes
        } returns List(taxes.size) { taxesPrediction }

        every {
            itemsPrediction.toExtractedItem()
        } returnsMany items

        every {
            invoiceDocumentPrediction.lineItems
        } returns List(items.size) { itemsPrediction }

        return invoiceDocumentPrediction
    }
}
