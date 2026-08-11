package printscript.readers

import printscript.common.domain.Token
import printscript.common.domain.TokenType
import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import java.util.Optional

class NumberReader : FormatReader {
    override fun read(firstChar: Char, reader: CodeReader): Token {
        val initialPos: CharPosition = reader.currentPosition()
        var text: String = firstChar.toString()
        while (true) {
            val current = reader.peek()
            if (current.isEmpty) return Token(TokenType.EOF, Optional.empty(), initialPos, initialPos)

            if (!current.get().isDigit() || current.get() != '.') break

            text += reader.read().get()
        }
        val finalPos: CharPosition = reader.currentPosition()

        val periodCount: Int = text.count { ch -> ch == '.' }
        if (periodCount > 1) throw Error("Unexpected token in line ${finalPos.line} column ${finalPos.col}")

        return Token(TokenType.NUMBER_LITERAL, Optional.of(text), initialPos, finalPos)
    }
}
