package printscript.expression.call

import printscript.RawInputValue
import printscript.definitions.ReadInputEffect
import printscript.definitions.SideEffectManager
import printscript.error.MissingInput
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.syntax.SyntaxNode
import printscript.toPrintableString
import printscript.util.Result

/**
 * `readInput(<mensaje>)` — pide un valor por el canal de entrada.
 *
 * El texto vuelve crudo ([RawInputValue]): recién al asignarlo se sabe a qué
 * tipo convertirlo.
 */
object ReadInputCallHandler : CallHandler {
    override val callee = "readInput"

    override fun handle(
        result: EvalResult,
        node: SyntaxNode,
        effects: SideEffectManager,
    ): Result<EvalResult, RuntimeError> {
        val prompt = result.value.toPrintableString()
        val raw =
            effects.handle(ReadInputEffect(prompt))
                ?: return Result.Err(MissingInput(prompt, node.location))
        return Result.Ok(EvalResult(RawInputValue(raw), result.sideEffects))
    }
}
