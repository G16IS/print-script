package printscript.statement

import printscript.InterpreterContext
import printscript.SideEffect

data class StatementResult(
    val sideEffects: List<SideEffect>,
    val newContext: InterpreterContext,
)
