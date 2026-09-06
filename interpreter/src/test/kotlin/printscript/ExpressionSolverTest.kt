package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.error.DivisionByZero
import printscript.error.InvalidLiteral
import printscript.error.InvalidOperands
import printscript.error.NoNodeKindForNode
import printscript.error.UndeclaredIdentifier
import printscript.error.UnrecognizedNode
import printscript.error.UnresolvableCall
import printscript.error.UnresolvableExpression
import printscript.expression.DefaultExpressionSolver
import printscript.expression.literal.NumberLiteralEvaluator
import printscript.node.NodeKind
import printscript.node.NodeKindResolver
import printscript.node.PrintScriptMapping
import printscript.support.TEST_LOCATION
import printscript.support.binary
import printscript.support.call
import printscript.support.err
import printscript.support.identifierNode
import printscript.support.leaf
import printscript.support.node
import printscript.support.numberNode
import printscript.support.ok
import printscript.support.stringNode
import printscript.syntax.SyntaxNode

class ExpressionSolverTest {
    private val solver =
        DefaultExpressionSolver(
            NodeKindResolver(PrintScriptMapping.mapping),
            DefaultInterpreterFactory.defaultEvaluators(),
        )

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
        assertTrue(err(solver.solve(leafNumber("abc"), InterpreterContext())) is InvalidLiteral)
    }

    @Test
    fun `non finite number literal fails with InvalidLiteral`() {
        assertTrue(err(solver.solve(leafNumber("Infinity"), InterpreterContext())) is InvalidLiteral)
        assertTrue(err(solver.solve(leafNumber("NaN"), InterpreterContext())) is InvalidLiteral)
    }

    @Test
    fun `string literal strips surrounding quotes`() {
        val result = solver.solve(stringNode("hola"), InterpreterContext())

        assertEquals(StringValue("hola"), ok(result).value)
    }

    @Test
    fun `string literal without quotes fails with InvalidLiteral`() {
        val bare = leaf("string", "STRING_LITERAL", "hola")

        assertTrue(err(solver.solve(bare, InterpreterContext())) is InvalidLiteral)
    }

    @Test
    fun `identifier resolves to declared value`() {
        val context = InterpreterContext().declareVariable("pepe", StringValue("hola"))

        assertEquals(StringValue("hola"), ok(solver.solve(identifierNode("pepe"), context)).value)
    }

    @Test
    fun `undeclared identifier fails`() {
        assertTrue(err(solver.solve(identifierNode("nope"), InterpreterContext())) is UndeclaredIdentifier)
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
    fun `empty group fails with UnrecognizedNode`() {
        assertTrue(err(solver.solve(node("group"), InterpreterContext())) is UnrecognizedNode)
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
    fun `unknown callee fails with UnresolvableCall`() {
        val unknown = call(numberNode("1"), callee = "readInput")

        assertTrue(err(solver.solve(unknown, InterpreterContext())) is UnresolvableCall)
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
    fun `number node without token value fails with UnrecognizedNode`() {
        val bare = SyntaxNode("number", token = null, location = TEST_LOCATION)

        assertTrue(err(solver.solve(bare, InterpreterContext())) is UnrecognizedNode)
    }

    @Test
    fun `identifier node without token value fails with UnrecognizedNode`() {
        val bare = SyntaxNode("identifier", token = null, location = TEST_LOCATION)

        assertTrue(err(solver.solve(bare, InterpreterContext())) is UnrecognizedNode)
    }

    @Test
    fun `binary operator without token value fails with UnrecognizedNode`() {
        val expression =
            node(
                "expression",
                numberNode("1"),
                SyntaxNode("OPERATOR", token = null, location = TEST_LOCATION),
                numberNode("2"),
            )

        assertTrue(err(solver.solve(expression, InterpreterContext())) is UnrecognizedNode)
    }

    @Test
    fun `mapped kind without evaluator fails with UnresolvableExpression`() {
        val statementOnlyMapping = mapOf("weird" to NodeKind.VARIABLE_DECLARATION)
        val noStatementEvaluators =
            DefaultExpressionSolver(
                NodeKindResolver(statementOnlyMapping),
                listOf(NumberLiteralEvaluator),
            )

        val error = err(noStatementEvaluators.solve(node("weird", numberNode("1")), InterpreterContext()))

        assertTrue(error is UnresolvableExpression)
    }

    private fun leafNumber(text: String) = leaf("number", "NUMBER_LITERAL", text)
}
