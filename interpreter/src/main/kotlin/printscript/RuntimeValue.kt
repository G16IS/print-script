package printscript

sealed interface RuntimeValue

data class NumberValue(val value: Double): RuntimeValue
data class StringValue(val value: String): RuntimeValue
