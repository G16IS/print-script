package printscript.semantic

import printscript.common.ast.Location
import printscript.common.reader.CharPosition

data class SemanticError (
    val messageError: String,
    val location: Location
)

