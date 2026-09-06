package printscript.statement

import printscript.InterpreterContext
import printscript.SideEffect
import printscript.error.RuntimeError
import printscript.syntax.SyntaxNode
import printscript.util.Result

fun interface BlockExecutor {
    fun execute(
        statements: List<SyntaxNode>,
        context: InterpreterContext,
    ): Result<List<SideEffect>, RuntimeError>
}
