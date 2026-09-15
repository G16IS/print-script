package printscript

import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.expression.ExpressionSolver
import printscript.statement.BlockExecutor
import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxNode
import printscript.util.Result

class OnDemandInterpreter(
    private val expressionSolver: ExpressionSolver,
    statementExecutors: List<StatementExecutor>,
) : BlockExecutor {
    fun executeStatement(
        statement: SyntaxNode,
        context: InterpreterContext,
    ): Result<InterpreterContext, RuntimeError> {
        val executor =
            executorsByName[statement.name]
                ?: return Result.Err(UnresolvableExpression(statement.name, statement.location))
        return executor.execute(statement, context, expressionSolver, this)
    }

    override fun execute(
        statements: List<SyntaxNode>,
        context: InterpreterContext,
    ): Result<InterpreterContext, RuntimeError> {
        var current = context
        for (statement in statements) {
            current =
                when (val result = executeStatement(statement, current)) {
                    is Result.Ok -> result.value
                    is Result.Err -> return result
                }
        }
        return Result.Ok(current)
    }

    private val executorsByName: Map<String, StatementExecutor> =
        statementExecutors.flatMap { executor -> executor.nodeNames.map { it to executor } }.toMap()
}
