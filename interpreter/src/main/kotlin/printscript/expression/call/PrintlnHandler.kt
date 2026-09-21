package printscript.expression.call

import printscript.UnitValue
import printscript.definitions.PrintEffect
import printscript.definitions.SideEffectManager
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
        effects: SideEffectManager,
    ): Result<EvalResult, RuntimeError> {
        effects.handle(PrintEffect(result.value.toPrintableString()))
        return Result.Ok(EvalResult.pure(UnitValue))
    }
}
