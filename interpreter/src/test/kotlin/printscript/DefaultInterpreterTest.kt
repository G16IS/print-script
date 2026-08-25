package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.error.RuntimeError
import printscript.error.UndeclaredIdentifier
import printscript.expression.ExpressionSolver
import printscript.expression.GroupEvaluator
import printscript.expression.binaryoperation.BinaryOperationEvaluator
import printscript.expression.binaryoperation.DefaultTypeConfiguration
import printscript.expression.call.CallEvaluator
import printscript.expression.literal.IdentifierEvaluator
import printscript.expression.literal.NumberLiteralEvaluator
import printscript.expression.literal.StringLiteralEvaluator
import printscript.node.NodeKindResolver
import printscript.node.PrintScriptMapping
import printscript.statement.ExpressionStatementExecutor
import printscript.statement.VariableDeclarationExecutor
import printscript.support.call
import printscript.support.identifierNode
import printscript.support.leaf
import printscript.support.node
import printscript.support.numberNode
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result

class DefaultInterpreterTest {
    private val interpreter = DefaultInterpreterFactory.create()

    @Test
    fun `declaration threads the new context into later statements`() {
        val program =
            listOf(
                declaration("x", numberNode("1")),
                expressionStatement(call(identifierNode("x"))),
            )

        val effects = ok(interpreter.executeBlock(program, InterpreterContext()))

        assertEquals(listOf(PrintEffect("1")), effects)
    }

    @Test
    fun `redeclaration in the same scope shadows the previous value`() {
        val program =
            listOf(
                declaration("x", numberNode("1")),
                declaration("x", numberNode("2")),
                expressionStatement(call(identifierNode("x"))),
            )

        val effects = ok(interpreter.executeBlock(program, InterpreterContext()))

        assertEquals(listOf(PrintEffect("2")), effects)
    }

    @Test
    fun `effects accumulate in execution order`() {
        val program =
            listOf(
                expressionStatement(call(numberNode("1"))),
                expressionStatement(call(numberNode("2"))),
                declaration("unused", numberNode("3")),
            )

        val effects = ok(interpreter.executeBlock(program, InterpreterContext()))

        assertEquals(listOf(PrintEffect("1"), PrintEffect("2")), effects)
    }

    @Test
    fun `first runtime error aborts execution with Err`() {
        val program =
            listOf(
                expressionStatement(call(numberNode("1"))),
                expressionStatement(call(identifierNode("missing"))),
            )

        val result = interpreter.executeBlock(program, InterpreterContext())

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UndeclaredIdentifier)
    }

    @Test
    fun `empty program produces no effects`() {
        val result = interpreter.interpret(InterpreterContext(), SyntaxProgram.empty())

        assertTrue(result is Result.Ok)
        assertEquals(emptyList<SideEffect>(), (result as Result.Ok).value)
    }

    @Test
    fun `registering two executors for the same kind fails fast`() {
        assertThrows(IllegalStateException::class.java) {
            DefaultInterpreter(
                NodeKindResolver(PrintScriptMapping.mapping),
                solver(),
                listOf(VariableDeclarationExecutor(), VariableDeclarationExecutor()),
            )
        }
    }

    @Test
    fun `mapping a kind covered by no executor and no evaluator fails at construction`() {
        val resolver = NodeKindResolver(PrintScriptMapping.mapping)
        val solverWithoutCallEvaluators =
            ExpressionSolver(
                resolver,
                listOf(
                    NumberLiteralEvaluator(),
                    StringLiteralEvaluator(),
                    IdentifierEvaluator(),
                    GroupEvaluator(),
                    BinaryOperationEvaluator(DefaultTypeConfiguration()),
                ),
            )

        assertThrows(IllegalStateException::class.java) {
            DefaultInterpreter(
                resolver,
                solverWithoutCallEvaluators,
                listOf(VariableDeclarationExecutor(), ExpressionStatementExecutor()),
            )
        }
    }

    private fun declaration(
        name: String,
        value: SyntaxNode,
    ): SyntaxNode =
        node(
            "variable",
            leaf("ID", "ID", name),
            leaf("TYPE", "TYPE", "number"),
            node("expression", value),
        )

    private fun expressionStatement(expression: SyntaxNode): SyntaxNode =
        node("expression-stmt", node("expression", expression))

    private fun solver(): ExpressionSolver =
        ExpressionSolver(NodeKindResolver(PrintScriptMapping.mapping), evaluators())

    private fun evaluators() =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            IdentifierEvaluator(),
            GroupEvaluator(),
            BinaryOperationEvaluator(DefaultTypeConfiguration()),
            CallEvaluator(),
        )

    private fun <T> ok(result: Result<T, RuntimeError>) = (result as Result.Ok).value
}
