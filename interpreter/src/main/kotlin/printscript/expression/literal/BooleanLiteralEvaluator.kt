package printscript.expression.literal

import printscript.BooleanValue
import printscript.InterpreterContext
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

object BooleanLiteralEvaluator : ExpressionEvaluator {
    override val nodeNames = setOf(AstNames.BOOLEAN)

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        node.tokenValue().flatMap { text ->
            val boolean =
                text.toBooleanStrictOrNull()
                    ?: return@flatMap Result.Err(InvalidLiteral(text, node.location))
            Result.Ok(EvalResult.pure(BooleanValue(boolean)))
        }
}
