package printscript.expression

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.syntax.SyntaxNode
import printscript.util.Result

class DefaultExpressionSolver(
    evaluators: List<ExpressionEvaluator>,
) : ExpressionSolver {
    private val evaluatorsByName: Map<String, ExpressionEvaluator> =
        evaluators.flatMap { evaluator -> evaluator.nodeNames.map { it to evaluator } }.toMap()

    override fun solve(
        node: SyntaxNode,
        context: InterpreterContext,
    ): Result<EvalResult, RuntimeError> =
        evaluatorsByName[node.name]
            ?.evaluate(node, context, this)
            ?: Result.Err(UnresolvableExpression(node.name, node.location))
}
