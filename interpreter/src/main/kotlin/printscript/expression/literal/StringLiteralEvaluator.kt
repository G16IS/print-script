package printscript.expression.literal

import printscript.InterpreterContext
import printscript.StringValue
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.syntax.SyntaxNode
import printscript.util.Result

class StringLiteralEvaluator : ExpressionEvaluator {
    override val kind = NodeKind.STRING_LITERAL

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> {
        val raw = node.value()
        return Result.Ok(
            EvalResult
                .pure(StringValue(raw.removeSurrounding(QUOTE))),
        )
    }

    private companion object {
        const val QUOTE = "\""
    }
}
