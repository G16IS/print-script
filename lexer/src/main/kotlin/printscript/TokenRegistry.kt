package printscript

import printscript.domain.Token
import printscript.reader.CharPosition
import printscript.syntax.Location

class TokenRegistry(
    val wordMap: Map<String, (Location) -> Token>,
) {
    fun hasToken(tokenString: String): Boolean = wordMap.contains(tokenString)

    fun getToken(
        tokenString: String,
        initialPos: CharPosition,
        finalPos: CharPosition,
    ): Token {
        val location = Location(initialPos, finalPos)
        val value =
            wordMap[tokenString]
                ?: throw IllegalArgumentException(
                    "Unexpected token in line ${initialPos.line} column ${initialPos.col}",
                )

        return value.invoke(location)
    }
}
