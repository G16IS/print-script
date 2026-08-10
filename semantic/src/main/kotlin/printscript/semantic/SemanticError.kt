package printscript.semantic

import printscript.common.ast.Location

data class SemanticError(
    val messageError: String,
    val location: Location,
)

