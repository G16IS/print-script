package printscript.statement

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.error.UnrecognizedNode
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

/**
 * `let <id>: <type> = <expr>;`
 *
 * The declared type is ignored here: checking initializer/type compatibility is
 * the type-checker's job. The interpreter trusts a validated program.
 */
class VariableDeclarationExecutor : StatementExecutor {
    override val kind = NodeKind.VARIABLE_DECLARATION

    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<StatementResult, RuntimeError> =
        childOrError(node, NAME_NODE).flatMap { nameNode ->
            childOrError(node, EXPRESSION_NODE).flatMap { expressionNode ->
                solver.solve(expressionNode, context).map { result ->
                    StatementResult(
                        sideEffects = result.sideEffects,
                        newContext = context.declareVariable(nameNode.value(), result.value),
                    )
                }
            }
        }

    private fun childOrError(
        node: SyntaxNode,
        childName: String,
    ): Result<SyntaxNode, RuntimeError> =
        node
            .childOrNull(childName)
            ?.let { Result.Ok(it) }
            ?: Result.Err(UnrecognizedNode(node.name, node.location))

    private companion object {
        const val NAME_NODE = "ID"
        const val EXPRESSION_NODE = "expression"
    }
}
