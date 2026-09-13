package printscript

import printscript.error.RuntimeError
import printscript.error.UnresolvableExpression
import printscript.expression.ExpressionSolver
import printscript.statement.BlockExecutor
import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

class DefaultInterpreter(
    private val expressionSolver: ExpressionSolver,
    statementExecutors: List<StatementExecutor>,
    private val sideEffectManager: SideEffectManager,
) : Interpreter,
    BlockExecutor {
    override fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<Unit, RuntimeError> = execute(program.statements, context)

    override fun execute(
        statements: List<SyntaxNode>,
        context: InterpreterContext,
    ): Result<Unit, RuntimeError> {
        for (statement in statements) {
            executeStatement(statement, context)
        }
    }

    override fun executeStatement(
        statement: SyntaxNode,
        context: InterpreterContext,
    ): Result<Unit, RuntimeError> {
        val executor =
            executorsByName[statement.name]
                ?: return Result.Err(UnresolvableExpression(statement.name, statement.location))
        val executeResult = executor.execute(statement, context, expressionSolver)

        return executeResult.flatMap { statementResult ->
            statementResult.sideEffects.forEach { sideEffect ->
                sideEffectManager.handle(
                    sideEffect,
                )
            }
        }
    }

    private val executorsByName: Map<String, StatementExecutor> =
        statementExecutors.flatMap { executor -> executor.nodeNames.map { it to executor } }.toMap()
}
