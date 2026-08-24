package printscript.expression.literal

import printscript.InterpreterContext
import printscript.NumberValue
import printscript.error.InvalidLiteral
import printscript.error.TypeError
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.syntax.SyntaxNode
import printscript.util.Result

class NumberLiteralEvaluator : ExpressionEvaluator {
    override val kind = NodeKind.NUMBER_LITERAL

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, TypeError> {
        val text = node.value()
        val number =
            text.toDoubleOrNull()
                ?: return Result.Err(InvalidLiteral(text, node.location))
        return Result.Ok(EvalResult.pure(NumberValue(number)))
    }
}
