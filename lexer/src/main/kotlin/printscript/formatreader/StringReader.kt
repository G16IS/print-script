package printscript.formatreader

import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import printscript.lexer.Token
import printscript.lexer.TokenType
import java.util.Optional

class StringReader : FormatReader {
    override fun read(firstChar: Char, reader: CodeReader): Token {
        val initialPos = reader.currentPosition()
        val firstOptional = reader.read()
        val first: Char = if(firstOptional.isPresent) firstOptional.get() else throw Error("String expected")
        var text: String = first.toString()
        val closingChar: Char = if (firstChar == '"') '"' else '\''

        while (true) {
            val current = reader.peek()
            if (current.isEmpty) return Token(TokenType.Eof(), Optional.empty(), initialPos, initialPos)
            if (current.get() == closingChar) {
                reader.read()
                break
            }

            text += reader.read().get()
        }
        val finalPos: CharPosition = reader.currentPosition()
        return Token(TokenType.StringLiteral(), Optional.of(text), initialPos, finalPos)
    }
}
