package printscript.formatreader

import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import printscript.lexer.Eof
import printscript.lexer.Identifier
import printscript.lexer.Token
import printscript.tokenregistry.TokenRegistry
import java.util.Optional

class IdentifierReader(val tokenRegistry: TokenRegistry): FormatReader {
    override fun read(reader: CodeReader): Token {
        val initialPos: CharPosition = reader.currentPosition()
        var text: String = reader.peek().get().toString()

        while (true) {
            val current = reader.peek()
            if (current.isEmpty) return Token(Eof(), Optional.empty(), initialPos, initialPos)

            if (!current.get().isLetterOrDigit() && current.get() != '_') break

            text += reader.read().get()

        }
        val finalPos: CharPosition = reader.currentPosition();

        if (tokenRegistry.hasToken(text)) {
            return tokenRegistry.getToken(text, initialPos, finalPos)
        }

        return Token(Identifier(), Optional.of(text), initialPos, finalPos)
    }
}
