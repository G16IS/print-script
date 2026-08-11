package printscript

import printscript.common.ast.Location

data class SemanticError(
    val messageError: String,
    val location: Location,
)

