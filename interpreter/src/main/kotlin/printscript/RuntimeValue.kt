package printscript

import kotlin.math.floor

sealed interface RuntimeValue

data class NumberValue(
    val value: Double,
) : RuntimeValue

data class StringValue(
    val value: String,
) : RuntimeValue

data class BooleanValue(
    val value: Boolean,
) : RuntimeValue

/**
 * Texto leído por `readInput` / `readEnv`, todavía sin interpretar.
 *
 * Su tipo lo decide el destino (la anotación de la declaración o el tipo de la
 * variable asignada), así que el valor viaja crudo hasta ese punto y ahí se
 * convierte con [ValueCoercion]. Donde no hay destino declarado —dentro de un
 * `println`, o como operando de un binario— vale como string.
 */
data class RawInputValue(
    val raw: String,
) : RuntimeValue

data object UnitValue : RuntimeValue

data object UninitializedValue : RuntimeValue

fun RuntimeValue.toPrintableString(): String =
    when (this) {
        is StringValue -> value
        is NumberValue -> formatNumber(value)
        is BooleanValue -> value.toString()
        is RawInputValue -> raw
        UnitValue -> ""
        UninitializedValue -> ""
    }

private fun formatNumber(value: Double): String =
    if (!value.isInfinite() && !value.isNaN() && value == floor(value)) {
        value.toLong().toString()
    } else {
        value.toString()
    }
