package com.fabien.invoiceExtraction.mindee

import com.fabien.invoiceExtraction.ExtractedInvoice
import com.fabien.invoiceExtraction.ExtractedItem
import com.fabien.invoiceExtraction.ExtractedSupplier
import com.fabien.invoiceExtraction.ExtractedTax
import com.mindee.MindeeClient
import com.mindee.parsing.common.field.CompanyRegistrationField
import com.mindee.parsing.common.field.TaxField
import com.mindee.parsing.invoice.InvoiceLineItem
import com.mindee.parsing.invoice.InvoiceV4DocumentPrediction
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.datetime.toKotlinLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class MindeeApiTest {

    @MockK
    lateinit var client: MindeeClient

    private fun taxes(): List<Arguments> {
        return listOf(
            // TaxField, ExtractedTax
            Arguments.of(createTaxField(0.055, 25.01), ExtractedTax(0.055, 25.01)),
            Arguments.of(createTaxField(null, 25.01), ExtractedTax(null, 25.01)),
            Arguments.of(createTaxField(0.20, null), ExtractedTax(0.20, null)),
            Arguments.of(createTaxField(null, null), ExtractedTax(null, null)),
        )
    }

    @ParameterizedTest
    @MethodSource("taxes")
    fun `should extract tax from mindee response`(taxField: TaxField, expected: ExtractedTax) {
        with(MindeeApi(client)) {
            assertEquals(expected, taxField.toExtractedTax())
        }
    }

    private fun nationalIds(): List<Arguments> {
        val siren = "SIREN"
        val siret = "SIRET"
        val sirenRegistration = createCompanyRegistrationField(siren, MindeeCompanyRegistrationType.SIREN.type)
        val sirenRegistrationWithoutValue = createCompanyRegistrationField(null, MindeeCompanyRegistrationType.SIREN.type)
        val siretRegistration = createCompanyRegistrationField(siret, MindeeCompanyRegistrationType.SIRET.type)
        val siretRegistrationWithoutValue = createCompanyRegistrationField(null, MindeeCompanyRegistrationType.SIRET.type)
        return listOf(
            // list of registrations, result
            Arguments.of(emptyList<CompanyRegistrationField>(), null),
            Arguments.of(listOf(sirenRegistrationWithoutValue, siretRegistrationWithoutValue), null),
            Arguments.of(listOf(sirenRegistration), siren),
            Arguments.of(listOf(sirenRegistration, siretRegistrationWithoutValue), siren),
            Arguments.of(listOf(siretRegistration), siret),
            Arguments.of(listOf(sirenRegistrationWithoutValue, siretRegistration), siret),
        )
    }

    @ParameterizedTest
    @MethodSource("nationalIds")
    fun `should compute national id with siret higher order`(registrations: List<CompanyRegistrationField>, expected: String?) {
        with(MindeeApi(client)) {
            assertEquals(expected, registrations.toNationalId())
        }
    }

    private fun suppliers(): List<Arguments> {
        val siret = createCompanyRegistrationField("SIRET", MindeeCompanyRegistrationType.SIRET.type)
        val siren = createCompanyRegistrationField("SIRET", MindeeCompanyRegistrationType.SIREN.type)
        val vatNumber = createCompanyRegistrationField("VAT_NUMBER", MindeeCompanyRegistrationType.VAT_NUMBER.type)
        return listOf(
            // name, address, registration, result
            Arguments.of(
                "BurgerKing",
                "1 rue de la frite",
                listOf(siren, siret, vatNumber),
                ExtractedSupplier("BurgerKing", "1 rue de la frite", siret.value, vatNumber.value),
            ),
            Arguments.of("BurgerKing", "1 rue de la frite", listOf(siren), ExtractedSupplier("BurgerKing", "1 rue de la frite", siren.value, null)),
            Arguments.of("BurgerKing", "1 rue de la frite", listOf(vatNumber), ExtractedSupplier("BurgerKing", "1 rue de la frite", null, vatNumber.value)),
            Arguments.of(null, null, emptyList<CompanyRegistrationField>(), ExtractedSupplier(null, null, null, null)),
        )
    }

    @ParameterizedTest
    @MethodSource("suppliers")
    fun `should extract supplier from mindee response`(
        name: String?,
        address: String?,
        registrations: List<CompanyRegistrationField>,
        expected: ExtractedSupplier,
    ) {
        val invoiceDocumentPrediction = createInvoiceDocumentPredictionForSupplier(name, address, registrations)
        with(MindeeApi(client)) {
            assertEquals(expected, invoiceDocumentPrediction.getExtractedSupplier())
        }
    }

    private fun items(): List<Arguments> {
        val code = "9999"
        val description = "French Fries"
        val quantity = 3.555
        val totalExcl = 1.98
        val taxRate = 0.055
        return listOf(
            // code, description, quantity, totalExcl, taxRate, result
            Arguments.of(
                code,
                description,
                quantity,
                totalExcl,
                taxRate,
                ExtractedItem(code, description, quantity, totalExcl, taxRate),
            ),
            Arguments.of(
                null,
                null,
                null,
                null,
                null,
                ExtractedItem(null, null, null, null, null),
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
        expected: ExtractedItem,
    ) {
        val invoiceLineItem = createItem(code, description, quantity, totalExcl, taxRate)
        with(MindeeApi(client)) {
            assertEquals(expected, invoiceLineItem.toExtractedItem())
        }
    }

    private fun invoices(): List<Arguments> {
        val item1 = ExtractedItem("9999", "French Fries", 3.555, 1.98, 0.055)
        val item2 = ExtractedItem(null, "Nuggets", 18.0, null, 0.10)
        val supplier = ExtractedSupplier("BurgerKing", "1 rue de la frite", "SIRET", "VAT_NUMBER")
        val date = LocalDate.of(2023, 1, 18)
        val invoiceNumber = "123456789"
        val totalExcl = 18.98
        val totalIncl = 21.89
        val tax1 = ExtractedTax(rate = 0.055, amount = 0.11)
        val tax2 = ExtractedTax(rate = 0.10, null)

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
                ExtractedInvoice(date.toKotlinLocalDate(), invoiceNumber, supplier, totalExcl, totalIncl, listOf(tax1, tax2), listOf(item1, item2)),

            ),
        )
    }

    @ParameterizedTest
    @MethodSource("invoices")
    fun `should extract invoice from mindee response`(
        date: LocalDate?,
        number: String?,
        supplier: ExtractedSupplier,
        totalExcl: Double?,
        totalIncl: Double?,
        taxes: List<ExtractedTax>,
        items: List<ExtractedItem>,
        expected: ExtractedInvoice,
    ) {
        with(spyk(MindeeApi(client))) {
            val invoiceDocumentPrediction = createInvoiceDocumentPredictionForInvoice(date, number, supplier, totalExcl, totalIncl, taxes, items)

            val result = invoiceDocumentPrediction.toExtractedInvoice()
            verify {
                invoiceDocumentPrediction.getExtractedSupplier()
            }
            assertEquals(expected, result)
        }
    }

    private fun createTaxField(rate: Double?, amount: Double?): TaxField {
        val taxField = mockk<TaxField>()
        every {
            taxField.rate
        } returns rate

        every {
            taxField.value
        } returns amount

        return taxField
    }

    private fun createInvoiceDocumentPredictionForSupplier(
        name: String?,
        address: String?,
        registrations: List<CompanyRegistrationField>,
    ): InvoiceV4DocumentPrediction {
        val invoiceDocumentPrediction = mockk<InvoiceV4DocumentPrediction>()
        every {
            invoiceDocumentPrediction.supplierName.value
        } returns name

        every {
            invoiceDocumentPrediction.supplierAddress.value
        } returns address

        every {
            invoiceDocumentPrediction.supplierCompanyRegistrations
        } returns registrations

        return invoiceDocumentPrediction
    }

    private fun createCompanyRegistrationField(value: String?, type: String?): CompanyRegistrationField {
        val companyRegistrationField = mockk<CompanyRegistrationField>()
        every {
            companyRegistrationField.value
        } returns value

        every {
            companyRegistrationField.type
        } returns type

        return companyRegistrationField
    }

    private fun createItem(code: String?, description: String?, quantity: Double?, totalExcl: Double?, taxRate: Double?): InvoiceLineItem {
        val item = mockk<InvoiceLineItem>()
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

        return item
    }

    context(MindeeApi)
    private fun createInvoiceDocumentPredictionForInvoice(
        date: LocalDate?,
        number: String?,
        supplier: ExtractedSupplier,
        totalExcl: Double?,
        totalIncl: Double?,
        taxes: List<ExtractedTax>,
        items: List<ExtractedItem>,
    ): InvoiceV4DocumentPrediction {
        val invoiceDocumentPrediction = mockk<InvoiceV4DocumentPrediction>()
        val taxesPrediction = mockk<TaxField>()
        val itemsPrediction = mockk<InvoiceLineItem>()

        every {
            invoiceDocumentPrediction.invoiceDateField.value
        } returns date

        every {
            invoiceDocumentPrediction.invoiceNumber.value
        } returns number

        every {
            invoiceDocumentPrediction.getExtractedSupplier()
        } answers { supplier }

        every {
            invoiceDocumentPrediction.totalNet.value
        } returns totalExcl

        every {
            invoiceDocumentPrediction.totalAmount.value
        } returns totalIncl

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
