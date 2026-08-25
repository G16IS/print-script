package printscript.typechecker

import printscript.ast.Location

data class TypeError(
    val message: String,
    val location: Location,
)
