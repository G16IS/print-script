package printscript.error

import printscript.ast.Location

sealed interface LexerError : Error {
    val message: String
    val location: Location
}

data class UnexpectedToken(
    override val location: Location,
) : LexerError {
    override val message: String
        get() = "Unexpected token at line ${location.start.line} col ${location.start.col}"
}

data class UnexpectedEnfOfLine(
    override val location: Location,
    val wasReadingToken: String,
) : LexerError {
    override val message: String
        get() =
            "Unexpected end of file at line " +
                "${location.start.line} col " +
                "${location.start.col} " +
                "while reading token $wasReadingToken"
}
