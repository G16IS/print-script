package printscript.readers

import printscript.reader.CharPosition
import printscript.reader.CodeReader
import printscript.TokenRegistry
import printscript.domain.Token
import printscript.domain.TokenType
import java.util.Optional

class IdentifierReader(val tokenRegistry: TokenRegistry) : FormatReader {
    override fun read(firstChar: Char, reader: CodeReader): Token {
        val initialPos: CharPosition = reader.currentPosition()
        var text: String = firstChar.toString()

        while (true) {
            val current = reader.peek()
            if (current.isEmpty) return Token(TokenType.EOF, Optional.empty(), initialPos, initialPos)

            if (!current.get().isLetterOrDigit() && current.get() != '_') break

            text += reader.read().get()
        }
        val finalPos: CharPosition = reader.currentPosition()

        if (tokenRegistry.hasToken(text)) {
            return tokenRegistry.getToken(text, initialPos, finalPos)
        }

        return Token(TokenType.IDENTIFIER, Optional.of(text), initialPos, finalPos)
    }
}
