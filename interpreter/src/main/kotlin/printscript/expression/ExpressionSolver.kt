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
        evaluators.associateBy { it.kind }.also { byKind ->
            check(byKind.size == evaluators.size) {
                "Hay más de un ExpressionEvaluator registrado para el mismo NodeKind"
            }
        }

    fun solve(
        node: SyntaxNode,
        context: InterpreterContext,
    ): Result<EvalResult, RuntimeError> =
        nodeKindResolver.resolve(node).flatMap { kind ->
            evaluatorsByKind[kind]
                ?.evaluate(node, context, this)
                ?: Result.Err(UnresolvableExpression(node.name, node.location))
        }

    fun handledKinds(): Set<NodeKind> = evaluatorsByKind.keys
}
