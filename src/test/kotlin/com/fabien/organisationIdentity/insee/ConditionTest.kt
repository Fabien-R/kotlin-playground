package com.fabien.organisationIdentity.insee

import com.fabien.organisationIdentity.insee.CompositeCondition.And
import com.fabien.organisationIdentity.insee.CompositeCondition.Or
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
internal class ConditionTest {
    @BeforeEach
    fun clean() {
        unmockkAll()
    }

    private fun comparisonConditions(): List<Arguments> {
        return listOf(
            // message, condition, expected
            Arguments.of("Eq", Eq(InseeQueryFields.SIRET, "1234"), "siret:\"1234\""), //
            Arguments.of("NotEq", NotEq(InseeQueryFields.SIRET, "1234"), "-siret:\"1234\""), //
            Arguments.of("Contains", Contains(InseeQueryFields.SIRET, "1234"), "siret:*1234*"), //
            Arguments.of("ApproximateSearch", ApproximateSearch(InseeQueryFields.SIRET, "1234"), "siret:\"1234\"~2"), //
        )
    }

    private fun compositeConditions(): List<Arguments> {
        val eq = Eq(InseeQueryFields.SIRET, "1234")
        val contains = Eq(InseeQueryFields.NAME_LEGAL_UNIT, "Green")
        val notEq = Eq(InseeQueryFields.ZIP_CODE, "69000")
        val or = Or()
        or.addCondition(eq)
        or.addCondition(contains)
        return listOf(
            // message, condition, expected
            Arguments.of("zero condition", emptyList<Condition>(), fun(_: String) = ""), //
            Arguments.of("one simple condition", listOf(eq), fun(_: String) = eq.toString()), //
            Arguments.of(
                "several simple conditions",
                listOf(eq, contains, notEq),
                fun(operator: String) = "($eq $operator $contains $operator $notEq)",
            ), //
            Arguments.of(
                "nested conditions",
                listOf(or, notEq),
                fun(operator: String) = "(($eq ${or.operator} $contains) $operator $notEq)",
            ), //
        )
    }

    @ParameterizedTest
    @MethodSource("comparisonConditions")
    fun `Test comparison condition formatting`(message: String, condition: ComparisonCondition, expected: String) {
        assertEquals(expected, condition.toString(), message)
    }

    @ParameterizedTest
    @MethodSource("comparisonConditions")
    fun `Comparison condition can not add nested condition`(message: String, condition: ComparisonCondition) {
        assertThrows<IllegalStateException>("Can't add a nested condition to the insee comparison") {
            condition.addCondition(Eq(InseeQueryFields.SIRET, "WhatEver"))
        }
    }

    @ParameterizedTest
    @MethodSource("compositeConditions")
    fun `Test composite condition`(message: String, conditions: List<Condition>, expected: (operator: String) -> String) {
        val compositeCondition = And()
        conditions.forEach {
            compositeCondition.addCondition(it)
        }

        assertEquals(expected.invoke(compositeCondition.operator), compositeCondition.toString(), message)
    }

//    @TestFactory
//    fun testFunctionComparisonConditions()  {
//        val eq: Condition.(InseeQueryFields, Any) -> Unit = { field, value -> field eq value }
//        val notEq: Condition.(InseeQueryFields, Any) -> Unit = { field, value -> field notEq value }
//        val contains: Condition.(InseeQueryFields, Any) -> Unit = { field, value -> field contains value }
//        val approximateSearch: Condition.(InseeQueryFields, Any) -> Unit = { field, value -> field approximateSearch value }
//        listOf(
//            // message, comparisonCondition type, function
//            Triple("eq", Eq::class, eq),  //
//            Triple("notEq", NotEq::class, notEq),  //
//            Triple("contains", Contains::class, contains),  //
//            Triple("approximateSearch", ApproximateSearch::class, approximateSearch),  //
//        ).map { (message, clazz, function) ->
//            println("start")
//            DynamicTest.dynamicTest("Condition function $message should add its ComparisonCondition") {
//                `Condition comparison function should add their ComparisonCondition to condition`(clazz, function)
//            }
//            println("end")
//        }
//    }

    @Test
    fun eqFunctionShouldAddItsComparisonCondition() {
        val eq: Condition.(InseeQueryFields, Any) -> Unit = { field, value -> field eq value }
        `Condition comparison function should add its ComparisonCondition to condition`(Eq::class, eq)
    }

    @Test
    fun notEqFunctionShouldAddItsComparisonCondition() {
        val notEq: Condition.(InseeQueryFields, Any) -> Unit = { field, value -> field notEq value }
        `Condition comparison function should add its ComparisonCondition to condition`(NotEq::class, notEq)
    }

    @Test
    fun containsFunctionShouldAddItsComparisonCondition() {
        val contains: Condition.(InseeQueryFields, Any) -> Unit = { field, value -> field contains value }
        `Condition comparison function should add its ComparisonCondition to condition`(Contains::class, contains)
    }

    @Test
    fun approximateSearchFunctionShouldAddItsComparisonCondition() {
        val approximateSearch: Condition.(InseeQueryFields, Any) -> Unit = { field, value -> field approximateSearch value }
        `Condition comparison function should add its ComparisonCondition to condition`(ApproximateSearch::class, approximateSearch)
    }

    private inline fun <reified T : ComparisonCondition> `Condition comparison function should add its ComparisonCondition to condition`(
        clazz: KClass<T>,
        function: Condition.(field: InseeQueryFields, value: Any) -> Unit,
    ) {
        val value = "666"
        val field = InseeQueryFields.SIRET
        mockkConstructor(clazz)

        // /!\ We don't/can't mock constructor
        // We mock function of matching constructor to verify later it has been called.
        // Mockk does not check directly that a constructor has been called.
        every {
            constructedWith<T>(EqMatcher(field, true), EqMatcher(value, true)).toString()
        } answers { callOriginal() }

        val slot = slot<T>()

        val condition = spyk<Condition> {
            every {
                this@spyk.addCondition(capture(slot))
            } returns mockk()
            function(field, value)
        }

        slot.toString()

        verify(exactly = 1) {
            condition.addCondition(ofType(clazz))
        }

        verify(exactly = 1) {
            constructedWith<T>(EqMatcher(field, true), EqMatcher(value, true)).toString()
        }
    }

    @Test
    fun andFunctionShouldAddItsCompositeCondition() {
        val and: Condition.() -> Unit = { and { } }
        `Condition composite function should add its CompositeCondition to condition`(And::class, and)
    }

    @Test
    fun orFunctionShouldAddItsCompositeCondition() {
        val or: Condition.() -> Unit = { or { } }
        `Condition composite function should add its CompositeCondition to condition`(Or::class, or)
    }

    private inline fun <reified T : CompositeCondition> `Condition composite function should add its CompositeCondition to condition`(
        clazz: KClass<T>,
        function: Condition.() -> Unit,
    ) {
        mockkConstructor(clazz)

        // /!\ We don't/can't mock constructor
        // We mock function of matching constructor to verify later it has been called.
        // Mockk does not check directly that a constructor has been called.
        every {
            constructedWith<T>().toString()
        } answers { callOriginal() }

        val slot = slot<T>()

        val condition = spyk<Condition> {
            every {
                this@spyk.addCondition(capture(slot))
            } returns mockk()
            function()
        }

        slot.toString()

        verify(exactly = 1) {
            condition.addCondition(ofType(clazz))
        }

        verify(exactly = 1) {
            constructedWith<T>().toString()
        }
    }

    @Test
    fun `query should call InseeQueryBuilder with And composition condition`() {
        mockkConstructor(InseeQueryBuilder::class)
        mockkConstructor(And::class)

        every {
            constructedWith<InseeQueryBuilder>(OfTypeMatcher<And>(And::class)).build()
        } answers { callOriginal() }

        every {
            constructedWith<And>().addCondition(ofType(Eq::class))
        } answers { callOriginal() }

        every {
            constructedWith<And>().toString()
        } answers { callOriginal() }

        query {
            InseeQueryFields.SIRET eq "1234"
        }.build()

        verify(exactly = 1) {
            constructedWith<And>().toString()
            constructedWith<InseeQueryBuilder>(OfTypeMatcher<And>(And::class)).build()
        }
    }
}
