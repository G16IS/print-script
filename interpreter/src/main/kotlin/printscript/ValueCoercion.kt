package printscript

import printscript.error.InvalidLiteral
import printscript.error.RuntimeError
import printscript.syntax.Location
import printscript.util.Result

/**
 * Convierte el texto crudo de `readInput` / `readEnv` al tipo del destino.
 *
 * La consigna pide que la ejecución falle si el valor provisto no se puede
 * interpretar como el tipo esperado (`boolean` que recibe `"Hola"`).
 */
internal object ValueCoercion {
    private const val NUMBER = "number"
    private const val STRING = "string"
    private const val BOOLEAN = "boolean"

    /** Deja pasar cualquier valor que no sea crudo; sólo [RawInputValue] se convierte. */
    fun toDeclared(
        value: RuntimeValue,
        declaredType: String?,
        location: Location,
    ): Result<RuntimeValue, RuntimeError> =
        when {
            value !is RawInputValue -> Result.Ok(value)
            declaredType == null -> Result.Ok(StringValue(value.raw))
            else -> coerce(value.raw, declaredType, location)
        }

    private fun coerce(
        raw: String,
        declaredType: String,
        location: Location,
    ): Result<RuntimeValue, RuntimeError> {
        val coerced: RuntimeValue? =
            when (declaredType) {
                STRING -> StringValue(raw)
                NUMBER ->
                    raw
                        .trim()
                        .toDoubleOrNull()
                        ?.takeIf { it.isFinite() }
                        ?.let { NumberValue(it) }
                BOOLEAN -> raw.trim().toBooleanStrictOrNull()?.let { BooleanValue(it) }
                else -> null
            }
        return coerced?.let { Result.Ok(it) }
            ?: Result.Err(InvalidLiteral(raw, location))
    }
}
