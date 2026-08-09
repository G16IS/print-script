package printscript.semantic

import printscript.common.reader.CharPosition

data class SemanticError (
    val messageError: String,
    val startPosition: CharPosition,
    val endPosition: CharPosition,
)

