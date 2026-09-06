package printscript.statement

import printscript.InterpreterContext
import printscript.SideEffect
import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.expression.ExpressionSolver
import printscript.node.associateByNodeNames
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

class DefaultBlockExecutor(
    private val expressionSolver: ExpressionSolver,
    statementExecutors: List<StatementExecutor>,
) : BlockExecutor {
    private val executorsByName: Map<String, StatementExecutor> =
        statementExecutors.associateByNodeNames { it.nodeNames }

    override fun execute(
        statements: List<SyntaxNode>,
        context: InterpreterContext,
    ): Result<List<SideEffect>, RuntimeError> {
        val initial: Result<StatementResult, RuntimeError> =
            Result.Ok(StatementResult(emptyList(), context))

        return statements
            .fold(initial) { acc, statement ->
                acc.flatMap { state ->
                    executeSingle(statement, state.newContext).map { next ->
                        StatementResult(state.sideEffects + next.sideEffects, next.newContext)
                    }
                }
            }.map { it.sideEffects }
    }

    private fun executeSingle(
        statement: SyntaxNode,
        context: InterpreterContext,
    ): Result<StatementResult, RuntimeError> {
        val executor =
            executorsByName[statement.name]
                ?: return Result.Err(UnresolvableExpression(statement.name, statement.location))
        return executor.execute(statement, context, expressionSolver)
    }
}
