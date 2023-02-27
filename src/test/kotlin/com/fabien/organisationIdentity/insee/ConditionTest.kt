package com.fabien.organisationIdentity.insee

import com.fabien.organisationIdentity.insee.CompositeCondition.And
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals


internal class ConditionTest {
    companion object {
        @JvmStatic
        fun comparisonConditions(): List<Arguments> {
            return listOf(
                // message, condition, expected
                Arguments.of("Eq", Eq(InseeQueryFields.SIRET, "1234"), "siret:\"1234\""),  //
                Arguments.of("NotEq", NotEq(InseeQueryFields.SIRET, "1234"), "-siret:\"1234\""),  //
                Arguments.of("Contains", Contains(InseeQueryFields.SIRET, "1234"), "siret:*1234*"),  //
                Arguments.of("ApproximateSearch", ApproximateSearch(InseeQueryFields.SIRET, "1234"), "siret:\"1234\"~2"),  //
            )
        }

        @JvmStatic
        fun compositeConditions(): List<Arguments> {
            val eq = Eq(InseeQueryFields.SIRET, "1234")
            val contains = Eq(InseeQueryFields.NAME_LEGAL_UNIT, "Green")
            val notEq = Eq(InseeQueryFields.ZIP_CODE, "69000")
            return listOf(
                // message, condition, expected
                Arguments.of("zero condition", emptyList<Condition>(), fun(_: String) = ""),  //
                Arguments.of("one simple condition", listOf(eq), fun(_: String) = eq.toString()),  //
                Arguments.of(
                    "several simple conditions",
                    listOf(eq, contains, notEq),
                    fun(operator: String) = "($eq $operator $contains $operator $notEq)"),  //
            )
        }
    }

    @ParameterizedTest
    @MethodSource("comparisonConditions")
    fun `Test comparison condition`(message: String, condition: ComparisonCondition, expected: String) {
        assertEquals(expected, condition.toString(), message)
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
}