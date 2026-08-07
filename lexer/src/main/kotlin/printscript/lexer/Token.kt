package printscript.lexer

import printscript.common.reader.CharPosition
import java.util.Optional

data class Token(
    val type: TokenType,
    val value: Optional<String>,
    val start: CharPosition,
    val end: CharPosition
)
