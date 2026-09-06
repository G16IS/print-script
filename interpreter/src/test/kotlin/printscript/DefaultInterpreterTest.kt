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
import printscript.node.NodeKind
import printscript.node.NodeKindResolver
import printscript.node.PrintScriptMapping
import printscript.statement.ExpressionStatementExecutor
import printscript.statement.StatementExecutor
import printscript.statement.StatementResult
import printscript.statement.VariableDeclarationExecutor
import printscript.support.call
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
    private val interpreter = DefaultInterpreterFactory.create()

    @Test
    fun `declaration threads the new context into later statements`() {
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
    fun `redeclaration in the same scope shadows the previous value`() {
        val effects =
            ok(
                interpreter.interpret(
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
                interpreter.interpret(
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
            interpreter.interpret(
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
        val result = interpreter.interpret(InterpreterContext(), SyntaxProgram.empty())

        assertEquals(emptyList<SideEffect>(), ok(result))
    }

    @Test
    fun `registering two executors for the same kind uses the last one`() {
        val interpreter =
            DefaultInterpreter(
                NodeKindResolver(PrintScriptMapping.mapping),
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
    fun `mapped kind without executor or evaluator fails with UnresolvableExpression`() {
        val resolver = NodeKindResolver(PrintScriptMapping.mapping)
        val solverWithoutCall =
            DefaultExpressionSolver(
                resolver,
                DefaultInterpreterFactory.defaultEvaluators().filterNot { it.kind == NodeKind.CALL },
            )
        val interpreter =
            DefaultInterpreter(
                resolver,
                solverWithoutCall,
                DefaultInterpreterFactory.defaultStatementExecutors(),
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

    private fun solver(): ExpressionSolver =
        DefaultExpressionSolver(
            NodeKindResolver(PrintScriptMapping.mapping),
            DefaultInterpreterFactory.defaultEvaluators(),
        )

    private object FailingDeclarationExecutor : StatementExecutor {
        override val kind = NodeKind.VARIABLE_DECLARATION

        override fun execute(
            node: SyntaxNode,
            context: InterpreterContext,
            solver: ExpressionSolver,
        ): Result<StatementResult, RuntimeError> = Result.Err(UnrecognizedNode(node.name, node.location))
    }
}
