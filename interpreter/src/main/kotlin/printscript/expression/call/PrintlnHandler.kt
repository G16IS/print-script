package printscript.expression.call

import printscript.PrintEffect
import printscript.UnitValue
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.syntax.SyntaxNode
import printscript.toPrintableString
import printscript.util.Result

object PrintlnHandler : CallHandler {
    override val callee = "println"

    override fun handle(
        result: EvalResult,
        node: SyntaxNode,
    ): Result<EvalResult, RuntimeError> =
        Result.Ok(
            EvalResult(
                value = UnitValue,
                sideEffects = result.sideEffects + PrintEffect(result.value.toPrintableString()),
            ),
        )
}
