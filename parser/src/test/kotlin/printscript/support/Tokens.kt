package printscript.support

import java.util.Optional
import printscript.ast.Location
import printscript.domain.Token
import printscript.reader.CharPosition

object Tokens {
    private var col: Int = 1

    fun reset() {
        col = 1
    }

    fun of(
        type: String,
        value: String? = null,
    ): Token {
        val start = CharPosition(1, col)
        col += value?.length ?: 1
        val end = CharPosition(1, col)
        col += 1
        return Token(type, Optional.ofNullable(value), Location(start, end))
    }

    fun let() = of("LET")

    fun colon() = of("COLON")

    fun assign() = of("ASSIGN")

    fun semicolon() = of("SEMICOLON")

    fun lparen() = of("LEFT_PAREN")

    fun rparen() = of("RIGHT_PAREN")

    fun lbrace() = of("LEFT_BRACE")

    fun rbrace() = of("RIGHT_BRACE")

    fun eof() = of("EOF")

    fun print() = of("CALL", "println")

    fun id(name: String) = of("ID", name)

    fun number(value: String) = of("NUMBER_LITERAL", value)

    fun string(value: String) = of("STRING_LITERAL", value)

    fun type(name: String) = of("TYPE", name)

    fun op(symbol: String) = of("OPERATOR", symbol)
}
