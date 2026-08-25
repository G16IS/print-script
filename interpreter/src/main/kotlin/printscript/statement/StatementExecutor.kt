package printscript.statement

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface StatementExecutor {
    val kind: NodeKind

    fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<StatementResult, RuntimeError>
}
