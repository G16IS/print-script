package printscript.statement

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.node.firstChild
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

/**
 * `<expr>;` — the value is discarded; effects (e.g. from `println`) are kept.
 */
object ExpressionStatementExecutor : StatementExecutor {
    override val kind = NodeKind.EXPRESSION_STMT

    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<StatementResult, RuntimeError> =
        node.firstChild().flatMap { solver.solve(it, context) }.map { result ->
            StatementResult(sideEffects = result.sideEffects, newContext = context)
        }
}
