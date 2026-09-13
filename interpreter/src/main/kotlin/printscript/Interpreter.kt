package printscript

import printscript.error.RuntimeError
import printscript.statement.StatementResult
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result

interface Interpreter {
    fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<Unit, RuntimeError>

    fun executeStatement(
        statement: SyntaxNode,
        context: InterpreterContext,
    ): Result<StatementResult, RuntimeError>
}
