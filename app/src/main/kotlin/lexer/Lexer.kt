package lexer

import com.google.common.collect.ImmutableMap
import lexer.reader.Reader
import token.Assign
import token.Eof
import token.Identifier
import token.LeftParen
import token.NumberLiteral
import token.Operator
import token.Position
import token.RightParen
import token.Semicolon
import token.StringLiteral
import token.Token
import token.TokenType
import java.util.Optional

class Lexer(val reader: Reader, val reservedWords: ImmutableMap<String, TokenType>) {
    fun nextToken(): Token {
        val initialPos: Position = reader.currentPosition()
        val first = skipWhitespace()
        val firstChar: Char = if (first.isPresent) first.get() else return Token(Eof(), Optional.empty(), initialPos, initialPos)

        when(firstChar) {
            in 'a'..'z' , in 'A'..'Z' -> return readIdentifier(firstChar, initialPos)
            in '0'..'9' -> return readNumber(firstChar, initialPos)
            '"', '\'' -> return readString(firstChar, initialPos)
            '+','-','*','/' -> return Token(Operator(), Optional.of(first.toString()), initialPos, initialPos)
            '=' -> return Token(Assign(), Optional.empty(), initialPos, initialPos)
            '(' -> return Token(LeftParen(), Optional.empty(), initialPos, initialPos)
            ')' -> return Token(RightParen(), Optional.empty(), initialPos, initialPos)
            ';' -> return Token(Semicolon(), Optional.empty(), initialPos, initialPos)
            else -> throw Error("Unexpected character on line ${initialPos.line}")
        }
    }

    private fun skipWhitespace(): Optional<Char> {
        var current = reader.read()
        while (current.isPresent){
            if (!current.get().isWhitespace()){
                return current
            }
            current = reader.read()
        }
        return Optional.empty()
    }

    //idea: hacer que cada uno de estos sea una implementación de una interfaz
    private fun readIdentifier(first: Char, initialPos: Position): Token{
        var text: String = first.toString()

        while (true){
            val current = reader.peek()
            if (current.isEmpty) return Token(Eof(), Optional.empty(), initialPos, initialPos)

            if (!current.get().isLetterOrDigit() && current.get() != '_') break

            text += reader.read().get()

        }
        val finalPos: Position = reader.currentPosition();

        if (text in reservedWords) {
            val type: TokenType = reservedWords[text]!!
            return Token(type, Optional.empty(), initialPos, finalPos)
        }

        return Token(Identifier(), Optional.of(text), initialPos, finalPos)
    }

    private fun readNumber(first: Char, initialPos: Position): Token{
        var text: String = first.toString()

        while (true){
            val current = reader.peek()
            if (current.isEmpty) return Token(Eof(), Optional.empty(), initialPos, initialPos)

            if (!current.get().isDigit() || current.get() != '.') break

            text+= reader.read().get()
        }
        val finalPos: Position = reader.currentPosition()

        val periodCount: Int = text.count {ch -> ch == '.'}
        if (periodCount > 1 ) throw Error("Unexpected token in line ${finalPos.line} column ${finalPos.col}")

        return Token(NumberLiteral(), Optional.of(text), initialPos, finalPos)
    }

    private fun readString(first: Char, initialPos: Position): Token{
        var text: String = first.toString()
        val closingChar: Char = if (first == '"') '"' else '\''

        while (true){
            val current = reader.peek()
            if (current.isEmpty) return Token(Eof(), Optional.empty(), initialPos, initialPos)
            if (current.get() == closingChar) break

            text += reader.read().get()
        }
        val finalPos: Position = reader.currentPosition()
        return Token(StringLiteral(), Optional.of(text), initialPos, finalPos)
    }
}
