package printscript

import printscript.error.RuntimeError
import printscript.syntax.SyntaxProgram
import printscript.util.Result

interface Interpreter {
    fun interpret(
        context: InterpreterContext,
        program: SyntaxProgram,
    ): Result<List<SideEffect>, RuntimeError>
}
