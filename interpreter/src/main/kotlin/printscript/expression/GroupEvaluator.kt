package printscript.expression

import printscript.InterpreterContext
import printscript.error.TypeError
import printscript.error.UnresolvableExpression
import printscript.node.NodeKind
import printscript.syntax.SyntaxNode
import printscript.util.Result

/** `( expression )` — the parens are dropped by the parser, so just pass through. */
class GroupEvaluator : ExpressionEvaluator {
    override val kind = NodeKind.GROUP

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, TypeError> {
        val inner =
            node.children.firstOrNull()
                ?: return Result.Err(UnresolvableExpression(node.name, node.location))
        return solver.solve(inner, context)
    }
}
