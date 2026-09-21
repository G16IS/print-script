package printscript.statement

import printscript.InterpreterContext
import printscript.definitions.SideEffect

data class StatementResult(
    val sideEffects: List<SideEffect>,
    val newContext: InterpreterContext,
)
