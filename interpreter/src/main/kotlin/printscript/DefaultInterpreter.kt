package printscript

import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.statement.BlockExecutor
import printscript.statement.DefaultBlockExecutor
import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxProgram
import printscript.util.Result

class DefaultInterpreter(
    private val blockExecutor: BlockExecutor,
) : Interpreter {
    constructor(
        expressionSolver: ExpressionSolver,
        statementExecutors: List<StatementExecutor>,
    ) : this(DefaultBlockExecutor(expressionSolver, statementExecutors))

    override fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<List<SideEffect>, RuntimeError> = blockExecutor.execute(program.statements, context)
}
