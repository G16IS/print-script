package printscript

import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.expression.ExpressionSolver
import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxNode
import printscript.util.Result

class OnDemandInterpreter(
    private val expressionSolver: ExpressionSolver,
    statementExecutors: List<StatementExecutor>,
) {
    fun executeStatement(
        statement: SyntaxNode,
        context: InterpreterContext,
    ): Result<InterpreterContext, RuntimeError> {
        val executor =
            executorsByName[statement.name]
                ?: return Result.Err(UnresolvableExpression(statement.name, statement.location))
        return executor.execute(statement, context, expressionSolver)
    }

    private val executorsByName: Map<String, StatementExecutor> =
        statementExecutors.flatMap { executor -> executor.nodeNames.map { it to executor } }.toMap()
}
