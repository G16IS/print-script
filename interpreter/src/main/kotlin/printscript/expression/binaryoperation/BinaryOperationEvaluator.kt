package printscript.expression.binaryoperation

import printscript.InterpreterContext
import printscript.NumberValue
import printscript.RuntimeValue
import printscript.StringValue
import printscript.UnitValue
import printscript.error.DivisionByZero
import printscript.error.InvalidOperands
import printscript.error.RuntimeError
import printscript.error.UnrecognizedNode
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.node.childAt
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

/**
 * Evaluates infix nodes produced by LeftRuleHandler.
 *
 * The handler always wraps, so a node without operators has a single child and
 * behaves as a pass-through; with an operator the shape is [left, OPERATOR, right].
 */
class BinaryOperationEvaluator(
    private val typeConfiguration: TypeConfiguration,
) : ExpressionEvaluator {
    override val kind = NodeKind.BINARY_OP

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        when (node.children.size) {
            UNARY_CHILDREN ->
                node.childAt(ONLY_CHILD_INDEX).flatMap { solver.solve(it, context) }
            CHILDREN_WITH_OPERATOR ->
                node.childAt(OPERATOR_INDEX).flatMap { opNode ->
                    opNode.tokenValue().flatMap { operator ->
                        node.childAt(LEFT_INDEX).flatMap { leftNode ->
                            node.childAt(RIGHT_INDEX).flatMap { rightNode ->
                                solver.solve(leftNode, context).flatMap { left ->
                                    solver.solve(rightNode, context).flatMap { right ->
                                        apply(operator, left, right, node)
                                    }
                                }
                            }
                        }
                    }
                }
            else -> Result.Err(UnrecognizedNode(node.name, node.location))
        }

    private fun apply(
        operator: String,
        left: EvalResult,
        right: EvalResult,
        node: SyntaxNode,
    ): Result<EvalResult, RuntimeError> =
        when {
            dividesByZero(operator, right.value) ->
                Result
                    .Err(DivisionByZero(node.location))

            else -> {
                val rule =
                    typeConfiguration.resolveBinaryOperation(
                        operator,
                        left.value::class,
                        right.value::class,
                    )
                val computed = rule?.apply(left.value, right.value)
                if (computed != null) {
                    Result.Ok(EvalResult.combine(left, right, computed))
                } else {
                    Result.Err(
                        InvalidOperands(
                            operator,
                            displayName(left.value),
                            displayName(right.value),
                            node.location,
                        ),
                    )
                }
            }
        }

    private fun dividesByZero(
        operator: String,
        value: RuntimeValue,
    ): Boolean = operator == DIVISION && value is NumberValue && value.value == ZERO_DIVISOR

    private fun displayName(value: RuntimeValue): String =
        when (value) {
            is NumberValue -> "number"
            is StringValue -> "string"
            is UnitValue -> "unit"
        }

    private companion object {
        const val UNARY_CHILDREN = 1
        const val CHILDREN_WITH_OPERATOR = 3
        const val ONLY_CHILD_INDEX = 0
        const val LEFT_INDEX = 0
        const val OPERATOR_INDEX = 1
        const val RIGHT_INDEX = 2
        const val DIVISION = "/"
        const val ZERO_DIVISOR = 0.0
    }
}
