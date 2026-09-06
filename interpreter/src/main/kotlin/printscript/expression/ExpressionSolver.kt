package printscript.expression

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.node.NodeKind
import printscript.node.NodeKindResolver
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

class ExpressionSolver(
    private val nodeKindResolver: NodeKindResolver,
    evaluators: List<ExpressionEvaluator>,
) {
    private val evaluatorsByKind: Map<NodeKind, ExpressionEvaluator> =
        evaluators.associateBy { it.kind }

    fun solve(
        node: SyntaxNode,
        context: InterpreterContext,
    ): Result<EvalResult, RuntimeError> =
        nodeKindResolver.resolve(node).flatMap { kind ->
            evaluatorsByKind[kind]
                ?.evaluate(node, context, this)
                ?: Result.Err(UnresolvableExpression(node.name, node.location))
        }
}
