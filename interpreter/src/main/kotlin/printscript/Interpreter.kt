package printscript

import printscript.error.RuntimeError
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result

interface Interpreter {
    fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<InterpreterContext, RuntimeError>

    fun executeStatement(
        statement: SyntaxNode,
        context: InterpreterContext,
    ): Result<InterpreterContext, RuntimeError>
}
