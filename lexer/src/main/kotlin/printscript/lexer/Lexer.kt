package printscript.lexer

import kotlinx.collections.immutable.ImmutableMap
import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import java.util.Optional
import kotlin.text.isDigit
import kotlin.text.isLetterOrDigit
import kotlin.text.isWhitespace

class Lexer(val reader: CodeReader, val reservedWords: ImmutableMap<String, TokenType>) {
    fun nextToken(): Token {
        val initialPos: CharPosition = reader.currentPosition()
        val first = skipWhitespace()
        val firstChar: Char =
            if (first.isPresent) first.get() else return Token(Eof(), Optional.empty(), initialPos, initialPos)

        when (firstChar) {
            in 'a'..'z', in 'A'..'Z' -> return readIdentifier(firstChar, initialPos)
            in '0'..'9' -> return readNumber(firstChar, initialPos)
            '"', '\'' -> return readString(firstChar, initialPos)
            '+', '-', '*', '/' -> return Token(Operator(), Optional.of(first.toString()), initialPos, initialPos)
            '=' -> return Token(Assign(), Optional.empty(), initialPos, initialPos)
            '(' -> return Token(LeftParen(), Optional.empty(), initialPos, initialPos)
            ')' -> return Token(RightParen(), Optional.empty(), initialPos, initialPos)
            ';' -> return Token(Semicolon(), Optional.empty(), initialPos, initialPos)
            else -> throw Error("Unexpected character on line ${initialPos.line}")
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

    //idea: hacer que cada uno de estos sea una implementación de una interfaz
    private fun readIdentifier(first: Char, initialPos: CharPosition): Token {
        var text: String = first.toString()

        while (true) {
            val current = reader.peek()
            if (current.isEmpty) return Token(Eof(), Optional.empty(), initialPos, initialPos)

            if (!current.get().isLetterOrDigit() && current.get() != '_') break

            text += reader.read().get()

        }
        val finalPos: CharPosition = reader.currentPosition();

        if (reservedWords.contains(text)) {
            val type: TokenType = reservedWords[text]!!
            return Token(type, Optional.empty(), initialPos, finalPos)
        }

        return Token(Identifier(), Optional.of(text), initialPos, finalPos)
    }

    private fun readNumber(first: Char, initialPos: CharPosition): Token {
        var text: String = first.toString()

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

    private fun readString(first: Char, initialPos: CharPosition): Token {
        var text: String = first.toString()
        val closingChar: Char = if (first == '"') '"' else '\''

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
