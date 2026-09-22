package printscript.expression

import printscript.RuntimeValue
import printscript.definitions.SideEffect

/**
 * Result of evaluating an expression: its value plus any side effects produced
 * while computing it (e.g. a `println` call nested inside the expression).
 */
data class EvalResult(
    val value: RuntimeValue,
    val sideEffects: List<SideEffect>,
) {
    companion object {
        fun pure(value: RuntimeValue): EvalResult = EvalResult(value, emptyList())

        fun combine(
            left: EvalResult,
            right: EvalResult,
            value: RuntimeValue,
        ): EvalResult = EvalResult(value, left.sideEffects + right.sideEffects)
    }
}
