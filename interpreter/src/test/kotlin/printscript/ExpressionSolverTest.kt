package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.error.DivisionByZero
import printscript.error.InvalidLiteral
import printscript.error.InvalidOperands
import printscript.error.NoNodeKindForNode
import printscript.error.TypeError
import printscript.error.UndeclaredIdentifier
import printscript.error.UnresolvableExpression
import printscript.expression.EvalResult
import printscript.expression.ExpressionSolver
import printscript.expression.GroupEvaluator
import printscript.expression.binaryoperation.BinaryOperationEvaluator
import printscript.expression.binaryoperation.DefaultTypeConfiguration
import printscript.expression.call.CallEvaluator
import printscript.expression.literal.IdentifierEvaluator
import printscript.expression.literal.NumberLiteralEvaluator
import printscript.expression.literal.StringLiteralEvaluator
import printscript.node.NodeKind
import printscript.node.NodeKindResolver
import printscript.node.PrintScriptMapping
import printscript.support.binary
import printscript.support.call
import printscript.support.identifierNode
import printscript.support.node
import printscript.support.numberNode
import printscript.support.stringNode
import printscript.util.Result

class ExpressionSolverTest {
    private val solver = ExpressionSolver(NodeKindResolver(PrintScriptMapping.mapping), defaultEvaluators())

    @Test
    fun `number literal evaluates to NumberValue`() {
        val result = solver.solve(numberNode("42"), InterpreterContext())

        assertEquals(NumberValue(42.0), ok(result).value)
        assertTrue(ok(result).sideEffects.isEmpty())
    }

    @Test
    fun `decimal number literal keeps decimals`() {
        val result = solver.solve(numberNode("1.5"), InterpreterContext())

        assertEquals(NumberValue(1.5), ok(result).value)
    }

    @Test
    fun `malformed number literal fails with InvalidLiteral`() {
        val error = err(solver.solve(leafNumber("abc"), InterpreterContext()))

        assertTrue(error is InvalidLiteral)
    }

    @Test
    fun `string literal strips surrounding quotes`() {
        val result = solver.solve(stringNode("hola"), InterpreterContext())

        assertEquals(StringValue("hola"), ok(result).value)
    }

    @Test
    fun `identifier resolves to declared value`() {
        val context = InterpreterContext().declareVariable("pepe", StringValue("hola"))

        assertEquals(StringValue("hola"), ok(solver.solve(identifierNode("pepe"), context)).value)
    }

    @Test
    fun `undeclared identifier fails`() {
        val error = err(solver.solve(identifierNode("nope"), InterpreterContext()))

        assertTrue(error is UndeclaredIdentifier)
    }

    @Test
    fun `binary operation applies the matching rule`() {
        val expression = binary(numberNode("1"), "+", numberNode("2"))

        assertEquals(NumberValue(3.0), ok(solver.solve(expression, InterpreterContext())).value)
    }

    @Test
    fun `nested terms respect tree shape`() {
        val expression = binary(numberNode("1"), "+", binary(numberNode("2"), "*", numberNode("3"), "term"))

        assertEquals(NumberValue(7.0), ok(solver.solve(expression, InterpreterContext())).value)
    }

    @Test
    fun `single child node passes through without operator`() {
        val wrapped = node("expression", node("term", numberNode("7")))

        assertEquals(NumberValue(7.0), ok(solver.solve(wrapped, InterpreterContext())).value)
    }

    @Test
    fun `string plus string concatenates`() {
        val expression = binary(stringNode("a"), "+", stringNode("b"))

        assertEquals(StringValue("ab"), ok(solver.solve(expression, InterpreterContext())).value)
    }

    @Test
    fun `string and number addition is not defined at runtime`() {
        val expression = binary(stringNode("a"), "+", numberNode("1"))

        assertTrue(err(solver.solve(expression, InterpreterContext())) is InvalidOperands)
    }

    @Test
    fun `subtraction between number and string fails with InvalidOperands`() {
        val expression = binary(numberNode("1"), "-", stringNode("a"))

        assertTrue(err(solver.solve(expression, InterpreterContext())) is InvalidOperands)
    }

    @Test
    fun `division by zero fails with DivisionByZero`() {
        val expression = binary(numberNode("1"), "/", numberNode("0"), "term")

        assertTrue(err(solver.solve(expression, InterpreterContext())) is DivisionByZero)
    }

    @Test
    fun `group passes its inner expression through`() {
        val group = node("group", binary(numberNode("1"), "+", numberNode("2")))

        assertEquals(NumberValue(3.0), ok(solver.solve(group, InterpreterContext())).value)
    }

    @Test
    fun `println call yields UnitValue and a PrintEffect`() {
        val result = ok(solver.solve(call(numberNode("42")), InterpreterContext()))

        assertEquals(UnitValue, result.value)
        assertEquals(listOf(PrintEffect("42")), result.sideEffects)
    }

    @Test
    fun `println prints strings without quotes`() {
        val result = ok(solver.solve(call(stringNode("hola")), InterpreterContext()))

        assertEquals(listOf(PrintEffect("hola")), result.sideEffects)
    }

    @Test
    fun `nested calls accumulate effects in evaluation order`() {
        val nested = call(call(numberNode("1")))

        val result = ok(solver.solve(nested, InterpreterContext()))

        assertEquals(listOf(PrintEffect("1"), PrintEffect("")), result.sideEffects)
    }

    @Test
    fun `call used as operand of an arithmetic operation fails`() {
        val context = InterpreterContext().declareVariable("x", NumberValue(1.0))
        val expression = binary(numberNode("1"), "+", call(identifierNode("x")))

        assertTrue(err(solver.solve(expression, context)) is InvalidOperands)
    }

    @Test
    fun `node name outside the mapping fails with NoNodeKindForNode`() {
        val unknown = node("if", numberNode("1"))

        assertTrue(err(solver.solve(unknown, InterpreterContext())) is NoNodeKindForNode)
    }

    @Test
    fun `mapped kind without evaluator fails with UnresolvableExpression`() {
        val statementOnlyMapping = mapOf("weird" to NodeKind.VARIABLE_DECLARATION)
        val noStatementEvaluators =
            ExpressionSolver(
                NodeKindResolver(statementOnlyMapping),
                listOf(NumberLiteralEvaluator()),
            )

        val error = err(noStatementEvaluators.solve(node("weird", numberNode("1")), InterpreterContext()))

        assertTrue(error is UnresolvableExpression)
    }

    private fun defaultEvaluators() =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            IdentifierEvaluator(),
            GroupEvaluator(),
            BinaryOperationEvaluator(DefaultTypeConfiguration()),
            CallEvaluator(),
        )

    private fun leafNumber(text: String) = printscript.support.leaf("number", "NUMBER_LITERAL", text)

    private fun ok(result: Result<EvalResult, TypeError>) = (result as Result.Ok).value

    private fun err(result: Result<EvalResult, TypeError>) = (result as Result.Err).error
}
