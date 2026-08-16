package printscript.domain

import printscript.ast.Location
import printscript.reader.CharPosition
import java.util.Optional

data class Token(
    val type: String,
    val value: Optional<String>,
    val location: Location
)

