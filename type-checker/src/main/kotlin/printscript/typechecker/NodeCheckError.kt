package printscript.typechecker

import printscript.error.TypeError

data class NodeCheckError(
    val scope: ScopeStack,
    val error: TypeError,
)
