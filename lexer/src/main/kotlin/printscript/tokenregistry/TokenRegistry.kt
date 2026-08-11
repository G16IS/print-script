package printscript.tokenregistry

import printscript.common.ast.Location
import printscript.common.reader.CharPosition
import printscript.lexer.Token

class TokenRegistry(val wordMap: Map<String, (Location) -> Token>) {
    fun hasToken(tokenString: String): Boolean {
        return wordMap.contains(tokenString)
    }

    fun getToken(tokenString: String, initialPos: CharPosition, finalPos: CharPosition): Token{
        val location = Location(initialPos, finalPos)
        val value = wordMap[tokenString]?: throw Error("Unexpected token in line ${initialPos.line} column ${initialPos.col}")
        return value.invoke(location)
    }
}
