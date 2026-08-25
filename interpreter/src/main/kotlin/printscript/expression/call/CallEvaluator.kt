package printscript.expression.call

import printscript.InterpreterContext
import printscript.PrintEffect
import printscript.UnitValue
import printscript.error.RuntimeError
import printscript.error.UnrecognizedNode
import printscript.error.UnresolvableCall
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
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
        if (node.children.size < CHILDREN_COUNT) {
            Result.Err(UnrecognizedNode(node.name, node.location))
        } else {
            val callee = node.children[CALLEE_INDEX].value()
            solver.solve(node.children[ARGUMENT_INDEX], context).flatMap { result ->
                dispatch(callee, result, node)
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
        const val CHILDREN_COUNT = 2
        const val CALLEE_INDEX = 0
        const val ARGUMENT_INDEX = 1
        const val PRINTLN = "println"
    }
}
