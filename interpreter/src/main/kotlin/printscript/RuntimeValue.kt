package printscript

import kotlin.math.floor

sealed interface RuntimeValue

data class NumberValue(
    val value: Double,
) : RuntimeValue

data class StringValue(
    val value: String,
) : RuntimeValue

data object UnitValue : RuntimeValue

fun RuntimeValue.toPrintableString(): String =
    when (this) {
        is StringValue -> value
        is NumberValue -> formatNumber(value)
        UnitValue -> ""
    }

private fun formatNumber(value: Double): String =
    if (!value.isInfinite() && !value.isNaN() && value == floor(value)) {
        value.toLong().toString()
    } else {
        value.toString()
    }
