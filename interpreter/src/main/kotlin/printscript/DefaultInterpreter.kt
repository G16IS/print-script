package printscript

import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.expression.ExpressionSolver
import printscript.statement.BlockExecutor
import printscript.statement.StatementExecutor
import printscript.statement.StatementResult
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

class DefaultInterpreter(
    private val expressionSolver: ExpressionSolver,
    statementExecutors: List<StatementExecutor>,
) : Interpreter,
    BlockExecutor {
    private val executorsByName: Map<String, StatementExecutor> =
        statementExecutors.flatMap { executor -> executor.nodeNames.map { it to executor } }.toMap()

    override fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<List<SideEffect>, RuntimeError> = execute(program.statements, context)

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
