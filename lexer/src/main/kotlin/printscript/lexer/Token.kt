package printscript.lexer

import printscript.common.Position
import java.util.Optional

data class Token(
    val type: TokenType,
    val value: Optional<String>,
    val start: Position,
    val end: Position
)
