package printscript.statement

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.NodeKind
import printscript.node.namedChild
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map
import printscript.zip

/**
 * `let <id>: <type> = <expr>;`
 *
 * The declared type is ignored here: checking initializer/type compatibility is
 * the type-checker's job. The interpreter trusts a validated program.
 */
object VariableDeclarationExecutor : StatementExecutor {
    override val kind = NodeKind.VARIABLE_DECLARATION

    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<StatementResult, RuntimeError> =
        nameAndExpression(node).flatMap { (name, expression) ->
            solver.solve(expression, context).map { result ->
                StatementResult(
                    sideEffects = result.sideEffects,
                    newContext = context.declareVariable(name, result.value),
                )
            }
        }

    private fun nameAndExpression(node: SyntaxNode): Result<Pair<String, SyntaxNode>, RuntimeError> =
        node.namedChild(AstNames.ID).flatMap { it.tokenValue() }.zip(node.namedChild(AstNames.EXPRESSION))
}
