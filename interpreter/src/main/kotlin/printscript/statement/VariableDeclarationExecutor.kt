package printscript.statement

import printscript.InterpreterContext
import printscript.UninitializedValue
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.namedChild
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

/**
 * `let <id>: <type> (= <expr>)?;`
 *
 * The declared type is ignored here: checking initializer/type compatibility is
 * the type-checker's job. The interpreter trusts a validated program.
 */
object VariableDeclarationExecutor : StatementExecutor {
    override val nodeNames = setOf(AstNames.VARIABLE)

    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<InterpreterContext, RuntimeError> =
        node.namedChild(AstNames.ID).flatMap { it.tokenValue() }.flatMap { name ->
            val expression = node.findOrNull(AstNames.EXPRESSION)
            if (expression == null) {
                Result.Ok(context.declareVariable(name, UninitializedValue))
            } else {
                solver.solve(expression, context).map { result ->
                    context.declareVariable(name, result.value)
                }
            }
        }
}
