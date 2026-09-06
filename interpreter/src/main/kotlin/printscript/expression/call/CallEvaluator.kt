package printscript.expression.call

import printscript.InterpreterContext
import printscript.PrintEffect
import printscript.UnitValue
import printscript.error.RuntimeError
import printscript.error.UnresolvableCall
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.node.childAt
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.toPrintableString
import printscript.util.Result
import printscript.util.flatMap

/**
 * In this grammar a call is an expression (factor), so effects are produced
 * here and travel up wrapped in [EvalResult].
 */
class CallEvaluator : ExpressionEvaluator {
    override val kind = NodeKind.CALL

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        node.childAt(CALLEE_INDEX).flatMap { calleeNode ->
            node.childAt(ARGUMENT_INDEX).flatMap { argumentNode ->
                calleeNode.tokenValue().flatMap { callee ->
                    solver.solve(argumentNode, context).flatMap { result ->
                        dispatch(callee, result, node)
                    }
                }
            }
        }

    private fun dispatch(
        callee: String,
        result: EvalResult,
        node: SyntaxNode,
    ): Result<EvalResult, RuntimeError> =
        when (callee) {
            PRINTLN ->
                Result.Ok(
                    EvalResult(
                        value = UnitValue,
                        sideEffects = result.sideEffects + PrintEffect(result.value.toPrintableString()),
                    ),
                )
            else -> Result.Err(UnresolvableCall(callee, node.location))
        }

    private companion object {
        const val CALLEE_INDEX = 0
        const val ARGUMENT_INDEX = 1
        const val PRINTLN = "println"
    }
}
