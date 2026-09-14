package printscript

import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.expression.ExpressionSolver
import printscript.statement.BlockExecutor
import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result

class DefaultInterpreter(
    private val expressionSolver: ExpressionSolver,
    statementExecutors: List<StatementExecutor>,
) : Interpreter,
    BlockExecutor {
    override fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<InterpreterContext, RuntimeError> = execute(program.statements, context)

    override fun execute(
        statements: List<SyntaxNode>,
        context: InterpreterContext,
    ): Result<InterpreterContext, RuntimeError> {
        var contextCopy: InterpreterContext = context
        for (statement in statements) {
            contextCopy =
                when (val r = executeStatement(statement, contextCopy)) {
                    is Result.Ok -> r.value
                    is Result.Err -> return Result.Err(r.error)
                }
        }
        return Result.Ok(contextCopy)
    }

    override fun executeStatement(
        statement: SyntaxNode,
        context: InterpreterContext,
    ): Result<InterpreterContext, RuntimeError> {
        val executor =
            executorsByName[statement.name] ?: return Result
                .Err(
                    UnresolvableExpression(
                        statement.name,
                        statement.location,
                    ),
                )

        val executeResult =
            executor.execute(
                statement,
                context,
                expressionSolver,
            )

        return when (executeResult) {
            is Result.Ok -> Result.Ok(executeResult.value)
            is Result.Err -> Result.Err(executeResult.error)
        }
    }

    private val executorsByName: Map<String, StatementExecutor> =
        statementExecutors
            .flatMap { executor ->
                executor.nodeNames.map { it to executor }
            }.toMap()
}
