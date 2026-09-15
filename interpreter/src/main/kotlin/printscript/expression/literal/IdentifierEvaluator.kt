package printscript.expression.literal

import printscript.InterpreterContext
import printscript.UninitializedValue
import printscript.error.RuntimeError
import printscript.error.UndeclaredIdentifier
import printscript.error.UninitializedVariable
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

object IdentifierEvaluator : ExpressionEvaluator {
    override val nodeNames = setOf(AstNames.IDENTIFIER)

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        node.tokenValue().flatMap { name ->
            when (val value = context.getVariable(name)) {
                null -> Result.Err(UndeclaredIdentifier(name, node.location))
                UninitializedValue -> Result.Err(UninitializedVariable(name, node.location))
                else -> Result.Ok(EvalResult.pure(value))
            }
        }
}
