package printscript

import printscript.ast.Location

data class SemanticError(
    val messageError: String,
    val location: Location,
)
