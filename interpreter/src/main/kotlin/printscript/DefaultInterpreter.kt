package printscript

import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.node.NodeKindResolver
import printscript.statement.BlockExecutor
import printscript.statement.DefaultBlockExecutor
import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxProgram
import printscript.util.Result

class DefaultInterpreter(
    private val blockExecutor: BlockExecutor,
) : Interpreter {
    constructor(
        nodeKindResolver: NodeKindResolver,
        expressionSolver: ExpressionSolver,
        statementExecutors: List<StatementExecutor>,
    ) : this(DefaultBlockExecutor(nodeKindResolver, expressionSolver, statementExecutors))

    override fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<List<SideEffect>, RuntimeError> = blockExecutor.execute(program.statements, context)
}
