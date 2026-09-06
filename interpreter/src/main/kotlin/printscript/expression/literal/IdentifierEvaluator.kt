package printscript.expression.literal

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.error.UndeclaredIdentifier
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

object IdentifierEvaluator : ExpressionEvaluator {
    override val kind = NodeKind.IDENTIFIER

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        node.tokenValue().flatMap { name ->
            val value =
                context.getVariable(name)
                    ?: return@flatMap Result.Err(UndeclaredIdentifier(name, node.location))
            Result.Ok(EvalResult.pure(value))
        }
}
