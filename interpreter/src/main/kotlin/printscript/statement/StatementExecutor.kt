package printscript.statement

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface StatementExecutor {
    val nodeNames: Set<String>

    fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<InterpreterContext, RuntimeError>
}
