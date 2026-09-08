package printscript.expression

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface ExpressionEvaluator {
    val nodeNames: Set<String>

    fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError>
}
