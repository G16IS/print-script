package printscript.readers

import printscript.common.domain.Token
import printscript.common.domain.TokenType
import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import java.util.Optional

class TypeReader : FormatReader {
    override fun read(firstChar: Char, reader: CodeReader): Token {
        val initialPos: CharPosition = reader.currentPosition()
        val first: Optional<Char> = skipWhitespace(reader)
        if (first.isEmpty) throw Error("Unexpected token in line ${initialPos.line} column ${initialPos.col}")

        var text: String = first.get().toString()
        while (true) {
            val current = reader.peek()
            if (current.isEmpty) throw Error("Unexpected token in line ${initialPos.line} column ${initialPos.col}")

            if (!current.get().isLetterOrDigit() && current.get() != '_') break

            text += reader.read().get()
        }
        val finalPosition = reader.currentPosition()
        return Token(TokenType.TYPE, Optional.of(text), initialPos, finalPosition)
    }

    private fun skipWhitespace(reader: CodeReader): Optional<Char> {
        var current = reader.read()
        while (current.isPresent) {
            if (!current.get().isWhitespace()) {
                return current
            }
            current = reader.read()
        }
        return Optional.empty()
    }
}
