package printscript.lexer

import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import printscript.formatreader.IdentifierReader
import printscript.formatreader.NumberReader
import printscript.formatreader.StringReader
import printscript.formatreader.TypeReader
import printscript.tokenregistry.TokenRegistry
import java.util.Optional
import kotlin.text.isWhitespace

class TokenStream(private val reader: CodeReader, val mapper: TokenRegistry): Lexer {
    override fun nextToken(): Token {
        val initialPos: CharPosition = reader.currentPosition()
        val first = skipWhitespace()
        val firstChar: Char =
            if (first.isPresent) first.get() else return Token(Eof(), Optional.empty(), initialPos, initialPos)

        return when (firstChar) {
            in 'a'..'z', in 'A'..'Z' ->
                IdentifierReader(mapper).read(reader)

            in '0'..'9' ->
                NumberReader().read(reader)

            '"', '\'' ->
                StringReader().read(reader)

            '+', '-', '*', '/' ->
                Token(Operator(), Optional.of(firstChar.toString()), initialPos, initialPos)

            '=' ->
                Token(Assign(), Optional.empty(), initialPos, initialPos)

            '(' ->
                Token(LeftParen(), Optional.empty(), initialPos, initialPos)

            ')' ->
                Token(RightParen(), Optional.empty(), initialPos, initialPos)

            ';' ->
                Token(Semicolon(), Optional.empty(), initialPos, initialPos)

            ',' ->
                Token(Comma(), Optional.empty(), initialPos, initialPos)

            ':' ->
                TypeReader().read(reader)

            else ->
                throw Error("Unexpected character on line ${initialPos.line}")
        }
    }

    private fun skipWhitespace(): Optional<Char> {
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
