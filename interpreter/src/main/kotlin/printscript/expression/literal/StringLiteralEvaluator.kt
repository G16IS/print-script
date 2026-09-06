package printscript.expression.literal

import printscript.InterpreterContext
import printscript.StringValue
import printscript.error.InvalidLiteral
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

object StringLiteralEvaluator : ExpressionEvaluator {
    override val nodeNames = setOf(AstNames.STRING)

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        node.tokenValue().flatMap { raw ->
            if (raw.length >= 2 && raw.startsWith(QUOTE) && raw.endsWith(QUOTE)) {
                Result.Ok(EvalResult.pure(StringValue(raw.removeSurrounding(QUOTE))))
            } else {
                Result.Err(InvalidLiteral(raw, node.location))
            }
        }

    private const val QUOTE = "\""
}
