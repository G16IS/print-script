package printscript.error

import printscript.domain.Token

object ParseErrors {
    fun unexpectedToken(token: Token, expected: String): ParseException =
        ParseException(
            "Expected $expected, found ${token.type}",
            token.location
        )

    fun unexpectedStart(token: Token): ParseException =
        ParseException(
            "Unexpected token ${token.type}; expected start of statement",
            token.location
        )
}
