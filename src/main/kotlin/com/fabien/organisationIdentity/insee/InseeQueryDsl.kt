package com.fabien.organisationIdentity.insee

class InseeQueryBuilder(val condition: Condition) {
    fun build() = condition.toString()
}

fun query(initializer: Condition.()  -> Unit) = InseeQueryBuilder(CompositeCondition.And().apply(initializer))

abstract class Condition() {
    abstract fun addCondition(condition: Condition)
    infix fun InseeQueryFields.eq(value: Any) {
        addCondition(Eq(this, value))
    }

    infix fun InseeQueryFields.notEq(value: Any) {
        addCondition(NotEq(this, value))
    }

    infix fun InseeQueryFields.approximateSearch(value: Any) {
        addCondition(ApproximateSearch(this, value))
    }

    infix fun InseeQueryFields.contains(value: Any) {
        addCondition(Contains(this, value))
    }

    fun and(initializer: Condition.() -> Unit) {
        addCondition(CompositeCondition.And().apply(initializer))
    }

    fun or(initializer: Condition.() -> Unit) {
        addCondition(CompositeCondition.Or().apply(initializer))
    }
}

sealed class ComparisonCondition(open val field: InseeQueryFields, private val _value: Any, private val surround: Boolean = true) : Condition() {
    init {
        if (_value !is Number && _value !is String && _value !is Boolean) {
            throw IllegalArgumentException(
                "Only numbers, strings, and booleans values can be used in insee-queries"
            )
        }
    }

    override fun addCondition(condition: Condition) {
        throw IllegalStateException("Can't add a nested condition to the insee comparison")
    }

    internal val value: String
        get() = when (_value) {
            is String -> if (surround) "\"$_value\"" else _value// need surrounding double quote because may contain white spaces
            else -> _value.toString()
        }

}

class Eq(override val field: InseeQueryFields, _value: Any) : ComparisonCondition(field, _value) {
    override fun toString() = "${field.field}:$value"
}

class NotEq(override val field: InseeQueryFields, _value: Any) : ComparisonCondition(field, _value) {
    override fun toString() = "-${field.field}:$value"
}

class Contains(override val field: InseeQueryFields, _value: Any) : ComparisonCondition(field, _value, false) {
    override fun toString() = "${field.field}:*$value*"
}

class ApproximateSearch(override val field: InseeQueryFields, _value: Any) : ComparisonCondition(field, _value) {
    override fun toString() = "${field.field}:$value~2"
}


open class CompositeCondition private constructor(internal val operator: String) : Condition() {
    private val conditions = mutableListOf<Condition>()

    override fun addCondition(condition: Condition) {
        conditions += condition
    }

    override fun toString() =
        when(conditions.size) {
            0 -> ""
            1 -> conditions.first().toString()
            else -> conditions.joinToString(prefix = "(", postfix = ")", separator = " $operator ")
    }

    class And : CompositeCondition("AND")
    class Or : CompositeCondition("OR")
}