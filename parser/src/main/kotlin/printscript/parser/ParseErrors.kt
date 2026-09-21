package printscript.parser

import printscript.domain.Token
import printscript.error.MissingToken
import printscript.error.ParserError
import printscript.error.UnexpectedStart

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
