package printscript.expression.binaryoperation

import printscript.BooleanValue
import printscript.InterpreterContext
import printscript.NumberValue
import printscript.RawInputValue
import printscript.RuntimeValue
import printscript.StringValue
import printscript.UninitializedValue
import printscript.UnitValue
import printscript.error.DivisionByZero
import printscript.error.InvalidOperands
import printscript.error.RuntimeError
import printscript.error.UnrecognizedNode
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.childAt
import printscript.node.firstChild
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map
import printscript.zip

class BinaryOperationEvaluator(
    private val typeConfiguration: TypeConfiguration,
) : ExpressionEvaluator {
    override val nodeNames = setOf(AstNames.EXPRESSION, AstNames.TERM)

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        when (node.children.size) {
            UNARY_CHILDREN -> node.firstChild().flatMap { solver.solve(it, context) }
            CHILDREN_WITH_OPERATOR -> evaluateBinary(node, context, solver)
            else -> Result.Err(UnrecognizedNode(node.name, node.location))
        }

    private fun evaluateBinary(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        binaryParts(node).flatMap { parts ->
            evalOperands(parts.left, parts.right, context, solver).flatMap { (left, right) ->
                apply(parts.operator, left, right, node)
            }
        }

    private fun binaryParts(node: SyntaxNode): Result<BinaryParts, RuntimeError> =
        node.childAt(OPERATOR_INDEX).flatMap { it.tokenValue() }.flatMap { operator ->
            node.childAt(LEFT_INDEX).zip(node.childAt(RIGHT_INDEX)).map { (left, right) ->
                BinaryParts(operator, left, right)
            }
        }

    private fun evalOperands(
        leftNode: SyntaxNode,
        rightNode: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<Pair<EvalResult, EvalResult>, RuntimeError> =
        solver.solve(leftNode, context).flatMap { left ->
            solver.solve(rightNode, context).map { right -> left to right }
        }

    private fun apply(
        operator: String,
        left: EvalResult,
        right: EvalResult,
        node: SyntaxNode,
    ): Result<EvalResult, RuntimeError> {
        if (dividesByZero(operator, right.value)) {
            return Result.Err(DivisionByZero(node.location))
        }
        val leftValue = asOperand(left.value)
        val rightValue = asOperand(right.value)
        val computed =
            typeConfiguration
                .resolveBinaryOperation(operator, leftValue::class, rightValue::class)
                ?.apply(leftValue, rightValue)
        return if (computed != null) {
            Result.Ok(EvalResult.combine(left, right, computed))
        } else {
            Result.Err(
                InvalidOperands(
                    operator,
                    displayName(leftValue),
                    displayName(rightValue),
                    node.location,
                ),
            )
        }
    }

    /**
     * Sin un destino declarado, lo leído por `readInput` / `readEnv` vale como
     * string: la tabla de operaciones no necesita reglas propias para el crudo.
     */
    private fun asOperand(value: RuntimeValue): RuntimeValue =
        if (value is RawInputValue) StringValue(value.raw) else value

    private fun dividesByZero(
        operator: String,
        value: RuntimeValue,
    ): Boolean = operator == DIVISION && value is NumberValue && value.value == ZERO_DIVISOR

    private fun displayName(value: RuntimeValue): String =
        when (value) {
            is NumberValue -> "number"
            is StringValue -> "string"
            is BooleanValue -> "boolean"
            is RawInputValue -> "string"
            is UnitValue -> "unit"
            is UninitializedValue -> "uninitialized"
        }

    private data class BinaryParts(
        val operator: String,
        val left: SyntaxNode,
        val right: SyntaxNode,
    )

    private companion object {
        const val UNARY_CHILDREN = 1
        const val CHILDREN_WITH_OPERATOR = 3

        const val LEFT_INDEX = 0
        const val OPERATOR_INDEX = 1
        const val RIGHT_INDEX = 2

        const val DIVISION = "/"
        const val ZERO_DIVISOR = 0.0
    }
}
