package printscript.statement

import printscript.InterpreterContext
import printscript.SideEffect
import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.node.NodeKindResolver
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

class DefaultBlockExecutor(
    private val nodeKindResolver: NodeKindResolver,
    private val expressionSolver: ExpressionSolver,
    statementExecutors: List<StatementExecutor>,
) : BlockExecutor {
    private val executorsByKind: Map<NodeKind, StatementExecutor> =
        statementExecutors.associateBy { it.kind }

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
    ): Result<StatementResult, RuntimeError> =
        nodeKindResolver.resolve(statement).flatMap { kind ->
            val executor =
                executorsByKind[kind]
                    ?: return@flatMap Result.Err(UnresolvableExpression(statement.name, statement.location))
            executor.execute(statement, context, expressionSolver)
        }
}
