package printscript.typechecker

import printscript.syntax.Location

data class TypeError(
    val message: String,
    val location: Location,
)
