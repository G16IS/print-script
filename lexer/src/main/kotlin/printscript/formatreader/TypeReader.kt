package printscript.formatreader

import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import printscript.lexer.Token
import printscript.lexer.Type
import java.util.Optional

class TypeReader: FormatReader {
    override fun read(reader: CodeReader): Token {
        val first: Optional<Char> = skipWhitespace(reader)
        val initialPos: CharPosition = reader.currentPosition()
        if (first.isEmpty) throw Error("Unexpected token in line ${initialPos.line} column ${initialPos.col}")

        var text: String = first.get().toString()
        while (true) {
            val current = reader.peek()
            if (current.isEmpty) throw Error("Unexpected token in line ${initialPos.line} column ${initialPos.col}")

            if (!current.get().isLetterOrDigit() && current.get() != '_') break

            text += reader.read().get()
        }
        val finalPosition = reader.currentPosition()
        return Token(Type(), Optional.of(text), initialPos, finalPosition)

    }

    private fun skipWhitespace(reader: CodeReader): Optional<Char>{
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
