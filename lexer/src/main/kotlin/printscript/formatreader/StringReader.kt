package printscript.formatreader

import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import printscript.lexer.Eof
import printscript.lexer.StringLiteral
import printscript.lexer.Token
import java.util.Optional

class StringReader: FormatReader {
    override fun read(reader: CodeReader): Token {
        val first: Char = reader.peek().get()
        var text: String = first.toString()
        val closingChar: Char = if (first == '"') '"' else '\''
        val initialPos = reader.currentPosition()

        while (true) {
            val current = reader.peek()
            if (current.isEmpty) return Token(Eof(), Optional.empty(), initialPos, initialPos)
            if (current.get() == closingChar) break

            text += reader.read().get()
        }
        val finalPos: CharPosition = reader.currentPosition()
        return Token(StringLiteral(), Optional.of(text), initialPos, finalPos)
    }
}
