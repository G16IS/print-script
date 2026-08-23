package printscript

import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxProgram

class DefaultInterpreter(
    private val expressionSolver: ExpressionSolver,
    private val statementExecutor: List<StatementExecutor>
): Interpreter {
    override fun interpret(program: SyntaxProgram): List<SideEffect> {
        TODO("Not yet implemented")
    }
}
