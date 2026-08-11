package printscript.error


import printscript.domain.Token
import printscript.util.Locations

object ParseErrors {

    fun missingValue(token: Token, kind: String): ParseException =
        ParseException("$kind missing value", Locations.of(token))

    fun unexpectedToken(token: Token, expected: String): ParseException =
        ParseException(
            "Expected $expected, found ${token.type.name}",
            Locations.of(token)
        )

    fun expected(token: Token, message: String): ParseException =
        ParseException(message, Locations.of(token))
}