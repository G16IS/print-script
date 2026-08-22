package printscript.domain

import java.util.Optional
import printscript.ast.Location

data class Token(
    val type: String,
    val value: Optional<String>,
    val location: Location,
)
