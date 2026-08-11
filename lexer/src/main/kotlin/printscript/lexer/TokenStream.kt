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
    private val buffer = ArrayDeque<Token>();

    override fun nextToken(): Token = buffer.removeFirstOrNull()?: readNextToken()
    override fun peek(offset: Int?): Token {
        val realOffset: Int = offset?: 0

        while (buffer.size <= realOffset) buffer.addLast(readNextToken())
        return buffer[realOffset]
    }

    private fun readNextToken(): Token {
        val first = skipWhitespace()
        val initialPos: CharPosition = reader.currentPosition()
        val firstChar: Char =
            if (first.isPresent) first.get() else return Token(TokenType.Eof(), Optional.empty(), initialPos, initialPos)

        return when (firstChar) {
            in 'a'..'z', in 'A'..'Z' ->
                IdentifierReader(mapper).read(firstChar, reader)

            in '0'..'9' ->
                NumberReader().read(firstChar, reader)

            '"', '\'' ->
                StringReader().read(firstChar, reader)

            '+', '-', '*', '/' ->
                Token(TokenType.Operator(), Optional.of(firstChar.toString()), initialPos, initialPos)

            '=' ->
                Token(TokenType.Assign(), Optional.empty(), initialPos, initialPos)

            '(' ->
                Token(TokenType.LeftParen(), Optional.empty(), initialPos, initialPos)

            ')' ->
                Token(TokenType.RightParen(), Optional.empty(), initialPos, initialPos)

            ';' ->
                Token(TokenType.Semicolon(), Optional.empty(), initialPos, initialPos)

            ',' ->
                Token(TokenType.Comma(), Optional.empty(), initialPos, initialPos)

            ':' ->
                TypeReader().read(firstChar, reader)

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
