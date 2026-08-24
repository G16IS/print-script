package printscript.statement

import printscript.InterpreterContext
import printscript.error.TypeError
import printscript.error.UnresolvableExpression
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.map

/**
 * `<expr>;` — the value is discarded; effects (e.g. from `println`) are kept.
 */
class ExpressionStatementExecutor : StatementExecutor {
    override val kind = NodeKind.EXPRESSION_STMT

    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<StatementResult, TypeError> {
        val expressionNode =
            node.children.firstOrNull()
                ?: return Result.Err(UnresolvableExpression(node.name, node.location))

        return solver.solve(expressionNode, context).map { result ->
            StatementResult(sideEffects = result.sideEffects, newContext = context)
        }
    }
}
