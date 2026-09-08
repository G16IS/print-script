package printscript.expression.call

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.error.UnresolvableCall
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.childAt
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.zip

class CallEvaluator(
    handlers: List<CallHandler> = listOf(PrintlnHandler),
) : ExpressionEvaluator {
    override val nodeNames = setOf(AstNames.CALL)

    private val handlersByCallee: Map<String, CallHandler> = handlers.associateBy { it.callee }

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        calleeAndArgument(node).flatMap { (callee, argument) ->
            solver.solve(argument, context).flatMap { result ->
                handlersByCallee[callee]?.handle(result, node)
                    ?: Result.Err(UnresolvableCall(callee, node.location))
            }
        }

    private fun calleeAndArgument(node: SyntaxNode): Result<Pair<String, SyntaxNode>, RuntimeError> =
        node.childAt(CALLEE_INDEX).flatMap { it.tokenValue() }.zip(node.childAt(ARGUMENT_INDEX))

    private companion object {
        const val CALLEE_INDEX = 0
        const val ARGUMENT_INDEX = 1
    }
}
