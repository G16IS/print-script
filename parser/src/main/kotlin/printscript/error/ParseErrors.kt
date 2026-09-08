package printscript.error

import printscript.domain.Token

object ParseErrors {
    fun unexpectedToken(
        token: Token,
        expected: String,
    ): ParserError =
        MissingToken(
            token.location,
            "Expected $expected, found ${token.type}",
        )

    fun unexpectedStart(token: Token): ParserError =
        UnexpectedStart(
            token.location,
            "Unexpected token ${token.type}; expected start of statement",
        )
}
