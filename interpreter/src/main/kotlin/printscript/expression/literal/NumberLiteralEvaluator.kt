package printscript.expression.literal

import printscript.InterpreterContext
import printscript.NumberValue
import printscript.error.InvalidLiteral
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

class NumberLiteralEvaluator : ExpressionEvaluator {
    override val kind = NodeKind.NUMBER_LITERAL

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        node.tokenValue().flatMap { text ->
            val number =
                text.toDoubleOrNull()
                    ?: return@flatMap Result.Err(InvalidLiteral(text, node.location))
            Result.Ok(EvalResult.pure(NumberValue(number)))
        }
}
