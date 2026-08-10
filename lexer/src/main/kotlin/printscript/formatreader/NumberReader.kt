package printscript.formatreader

import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import printscript.lexer.Eof
import printscript.lexer.NumberLiteral
import printscript.lexer.Token
import java.util.Optional

class NumberReader: FormatReader {
    override fun read(reader: CodeReader): Token {
        val first: Char = reader.peek().get()
        var text: String = first.toString()
        val initialPos: CharPosition = reader.currentPosition()
        while (true) {
            val current = reader.peek()
            if (current.isEmpty) return Token(Eof(), Optional.empty(), initialPos, initialPos)

            if (!current.get().isDigit() || current.get() != '.') break

            text += reader.read().get()
        }
        val finalPos: CharPosition = reader.currentPosition()

        val periodCount: Int = text.count { ch -> ch == '.' }
        if (periodCount > 1) throw Error("Unexpected token in line ${finalPos.line} column ${finalPos.col}")

        return Token(NumberLiteral(), Optional.of(text), initialPos, finalPos)

    }
}
