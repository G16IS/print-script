package printscript.expression.call

import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface CallHandler {
    val callee: String

    fun handle(
        result: EvalResult,
        node: SyntaxNode,
    ): Result<EvalResult, RuntimeError>
}
