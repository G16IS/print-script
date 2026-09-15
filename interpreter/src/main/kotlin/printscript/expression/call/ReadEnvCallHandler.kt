package printscript.expression.call

import printscript.RawInputValue
import printscript.ReadEnvEffect
import printscript.SideEffectManager
import printscript.error.MissingEnvVariable
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.syntax.SyntaxNode
import printscript.toPrintableString
import printscript.util.Result

/**
 * `readEnv(<nombre>)` — lee una variable de ambiente.
 *
 * Igual que `readInput`, el valor vuelve crudo y se convierte en el destino.
 */
object ReadEnvCallHandler : CallHandler {
    override val callee = "readEnv"

    override fun handle(
        result: EvalResult,
        node: SyntaxNode,
        effects: SideEffectManager,
    ): Result<EvalResult, RuntimeError> {
        val name = result.value.toPrintableString()
        val raw =
            effects.handle(ReadEnvEffect(name))
                ?: return Result.Err(MissingEnvVariable(name, node.location))
        return Result.Ok(EvalResult(RawInputValue(raw), result.sideEffects))
    }
}
