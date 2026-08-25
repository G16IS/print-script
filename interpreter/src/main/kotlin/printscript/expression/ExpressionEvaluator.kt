package printscript.expression

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.node.NodeKind
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface ExpressionEvaluator {
    val kind: NodeKind

    fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError>
}
