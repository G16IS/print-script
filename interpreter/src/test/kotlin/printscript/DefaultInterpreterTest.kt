package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.error.RuntimeError
import printscript.error.UndeclaredIdentifier
import printscript.error.UnrecognizedNode
import printscript.error.UnresolvableExpression
import printscript.expression.DefaultExpressionSolver
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.statement.ExpressionStatementExecutor
import printscript.statement.StatementExecutor
import printscript.statement.StatementResult
import printscript.statement.VariableDeclarationExecutor
import printscript.support.call
import printscript.support.createInterpreter
import printscript.support.defaultEvaluators
import printscript.support.defaultExecutors
import printscript.support.err
import printscript.support.identifierNode
import printscript.support.leaf
import printscript.support.node
import printscript.support.numberNode
import printscript.support.ok
import printscript.support.program
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result

class DefaultInterpreterTest {
    @Test
    fun `declaration threads the new context into later statements`() {
        val effects =
            ok(
                createInterpreter("1.0").interpret(
                    InterpreterContext(),
                    program(
                        declaration("x", numberNode("1")),
                        expressionStatement(call(identifierNode("x"))),
                    ),
                ),
            )

        assertEquals(listOf(PrintEffect("1")), effects)
    }

    @Test
    fun `redeclaration in the same scope shadows the previous value`() {
        val effects =
            ok(
                createInterpreter("1.0").interpret(
                    InterpreterContext(),
                    program(
                        declaration("x", numberNode("1")),
                        declaration("x", numberNode("2")),
                        expressionStatement(call(identifierNode("x"))),
                    ),
                ),
            )

        assertEquals(listOf(PrintEffect("2")), effects)
    }

    @Test
    fun `effects accumulate in execution order`() {
        val effects =
            ok(
                createInterpreter("1.0").interpret(
                    InterpreterContext(),
                    program(
                        expressionStatement(call(numberNode("1"))),
                        expressionStatement(call(numberNode("2"))),
                        declaration("unused", numberNode("3")),
                    ),
                ),
            )

        assertEquals(listOf(PrintEffect("1"), PrintEffect("2")), effects)
    }

    @Test
    fun `first runtime error aborts execution with Err`() {
        val result =
            createInterpreter("1.0").interpret(
                InterpreterContext(),
                program(
                    expressionStatement(call(numberNode("1"))),
                    expressionStatement(call(identifierNode("missing"))),
                ),
            )

        assertTrue(err(result) is UndeclaredIdentifier)
    }

    @Test
    fun `empty program produces no effects`() {
        val result = createInterpreter("1.0").interpret(InterpreterContext(), SyntaxProgram.empty())

        assertEquals(emptyList<SideEffect>(), ok(result))
    }

    @Test
    fun `registering two executors for the same node name uses the last one`() {
        val interpreter =
            DefaultInterpreter(
                solver(),
                listOf(
                    FailingDeclarationExecutor,
                    VariableDeclarationExecutor,
                    ExpressionStatementExecutor,
                ),
            )

        val effects =
            ok(
                interpreter.interpret(
                    InterpreterContext(),
                    program(
                        declaration("x", numberNode("1")),
                        expressionStatement(call(identifierNode("x"))),
                    ),
                ),
            )

        assertEquals(listOf(PrintEffect("1")), effects)
    }

    @Test
    fun `call without evaluator fails with UnresolvableExpression`() {
        val solverWithoutCall =
            DefaultExpressionSolver(
                defaultEvaluators().filterNot { AstNames.CALL in it.nodeNames },
            )
        val interpreter =
            DefaultInterpreter(
                solverWithoutCall,
                defaultExecutors(),
            )

        val result =
            interpreter.interpret(
                InterpreterContext(),
                program(expressionStatement(call(numberNode("1")))),
            )

        assertTrue(err(result) is UnresolvableExpression)
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

    private fun solver(): ExpressionSolver = DefaultExpressionSolver(defaultEvaluators())

    private object FailingDeclarationExecutor : StatementExecutor {
        override val nodeNames = setOf(AstNames.VARIABLE)

        override fun execute(
            node: SyntaxNode,
            context: InterpreterContext,
            solver: ExpressionSolver,
        ): Result<StatementResult, RuntimeError> = Result.Err(UnrecognizedNode(node.name, node.location))
    }
}
