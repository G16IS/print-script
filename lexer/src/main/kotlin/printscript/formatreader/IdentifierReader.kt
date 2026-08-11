package printscript.formatreader

import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import printscript.lexer.Token
import printscript.lexer.TokenType
import printscript.tokenregistry.TokenRegistry
import java.util.Optional

class IdentifierReader(val tokenRegistry: TokenRegistry) : FormatReader {
    override fun read(firstChar: Char, reader: CodeReader): Token {
        val initialPos: CharPosition = reader.currentPosition()
        var text: String = firstChar.toString()

        while (true) {
            val current = reader.peek()
            if (current.isEmpty) return Token(TokenType.Eof(), Optional.empty(), initialPos, initialPos)

            if (!current.get().isLetterOrDigit() && current.get() != '_') break

            text += reader.read().get()

        }
        val finalPos: CharPosition = reader.currentPosition();

        if (tokenRegistry.hasToken(text)) {
            return tokenRegistry.getToken(text, initialPos, finalPos)
        }

        return Token(TokenType.Identifier(), Optional.of(text), initialPos, finalPos)
    }
}
