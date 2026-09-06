package printscript.expression

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.node.AstNames
import printscript.node.firstChild
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

/** `( expression )` — the parens are dropped by the parser, so just pass through. */
object GroupEvaluator : ExpressionEvaluator {
    override val nodeNames = setOf(AstNames.GROUP)

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> = node.firstChild().flatMap { solver.solve(it, context) }
}
