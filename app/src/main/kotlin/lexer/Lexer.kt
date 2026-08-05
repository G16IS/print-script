package lexer

import com.google.common.collect.ImmutableMap
import lexer.reader.Reader
import token.Assign
import token.Identifier
import token.LeftParen
import token.Let
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
    fun nextToken(): Token{
        val initialPos: Position = reader.getCurrentPosition()
        val first = skipWhitespace()
        val firstChar: Char = first.toChar()

        when(firstChar){
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

    private fun skipWhitespace(): Int {
        var current = reader.read()
        while (current != -1){
            val char = current.toChar()
            if (!char.isWhitespace()){
                return current
            }
            current = reader.read()
        }
        return -1
    }

    //idea: hacer que cada uno de estos sea una implementación de una interfaz
    private fun readIdentifier(first: Char, initialPos: Position): Token{
        var text: String = first.toString()
        var current: Char = reader.read().toChar()

        while (current.isLetter() || current.isDigit() || current == '_') {
            text += current
            current = reader.read().toChar()
        }
        val finalPos: Position = reader.getCurrentPosition();

        if (text in reservedWords) {
            val type: TokenType = reservedWords[text]!!
            return Token(type, Optional.empty(), initialPos, finalPos)
        }

        return Token(Identifier(), Optional.of(text), initialPos, finalPos)
    }

    private fun readNumber(first: Char, initialPos: Position): Token{
        var text: String = first.toString()
        var current = reader.read().toChar()

        while (current.isDigit() || current == '.'){
            text+= current
            current = reader.read().toChar()
        }
        val finalPos: Position = reader.getCurrentPosition()

        val periodCount: Int = text.count {ch -> ch == '.'}
        if (periodCount > 1 ) throw Error("Unexpected token in line ${finalPos.line} column ${finalPos.col}")

        return Token(NumberLiteral(), Optional.of(text), initialPos, finalPos)
    }

    private fun readString(first: Char, initialPos: Position): Token{
        var text: String = first.toString()
        var current = reader.read().toChar()
        val closingChar: Char = if (first == '"') '"' else '\''

        while (current != closingChar){
            text += current
            current = reader.read().toChar()
        }
        val finalPos: Position = reader.getCurrentPosition()
        return Token(StringLiteral(), Optional.of(text), initialPos, finalPos)
    }
}
