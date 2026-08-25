package printscript.expression.literal

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.error.UndeclaredIdentifier
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.syntax.SyntaxNode
import printscript.util.Result

class IdentifierEvaluator : ExpressionEvaluator {
    override val kind = NodeKind.IDENTIFIER

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> {
        val name = node.value()
        val value =
            context.getVariable(name)
                ?: return Result.Err(UndeclaredIdentifier(name, node.location))
        return Result.Ok(EvalResult.pure(value))
    }
}
