package printscript

import printscript.error.TypeError
import printscript.expression.ExpressionSolver
import printscript.node.NodeKind
import printscript.node.NodeKindResolver
import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

class DefaultInterpreter(
    private val nodeKindResolver: NodeKindResolver,
    private val expressionSolver: ExpressionSolver,
    statementExecutors: List<StatementExecutor>,
) : Interpreter {
    private val executorsByKind: Map<NodeKind, StatementExecutor> =
        statementExecutors.associateBy { it.kind }.also { byKind ->
            check(byKind.size == statementExecutors.size) {
                "Hay más de un StatementExecutor registrado para el mismo NodeKind"
            }
        }

    init {
        val covered = executorsByKind.keys + expressionSolver.handledKinds()
        val missing = nodeKindResolver.mapping.values.toSet() - covered
        check(missing.isEmpty()) {
            "No hay StatementExecutor ni ExpressionEvaluator registrado para los NodeKind $missing"
        }
    }

    override fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<List<SideEffect>, TypeError> = executeBlock(program.statements, context)

    fun executeBlock(
        statements: List<SyntaxNode>,
        context: InterpreterContext,
    ): Result<List<SideEffect>, TypeError> {
        val initialState: Result<BlockState, TypeError> = Result.Ok(BlockState(emptyList(), context))
        return statements
            .fold(initialState) { acc, statement ->
                acc.flatMap { state ->
                    executeSingle(statement, state.context).map { (effects, newContext) ->
                        BlockState(state.effects + effects, newContext)
                    }
                }
            }.map { it.effects }
    }

    private fun executeSingle(
        statement: SyntaxNode,
        context: InterpreterContext,
    ): Result<Pair<List<SideEffect>, InterpreterContext>, TypeError> =
        nodeKindResolver.resolve(statement).flatMap { kind ->
            executorsByKind.getValue(kind).execute(statement, context, expressionSolver).map { outcome ->
                outcome.sideEffects to outcome.newContext
            }
        }

    private data class BlockState(
        val effects: List<SideEffect>,
        val context: InterpreterContext,
    )
}
