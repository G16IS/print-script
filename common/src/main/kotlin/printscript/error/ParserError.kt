package printscript.error

import printscript.syntax.Location

sealed interface ParserError : Error {
    override val message: String
    val location: Location
}

data class MissingToken(
    override val location: Location,
    override val message: String =
        "Unexpected token at line ${location.start.line} col ${location.start.col}",
) : ParserError

data class UnexpectedStart(
    override val location: Location,
    override val message: String =
        "Unexpected token at line ${location.start.line} col ${location.start.col}",
) : ParserError
