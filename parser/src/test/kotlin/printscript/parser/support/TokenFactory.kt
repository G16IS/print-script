package printscript.parser.support

import printscript.common.reader.CharPosition
import printscript.common.domain.Assign
import printscript.lexer.Colon
import printscript.common.domain.Eof
import printscript.common.domain.Identifier
import printscript.common.domain.LeftParen
import printscript.common.domain.Let
import printscript.common.domain.NumberLiteral
import printscript.common.domain.Operator
import printscript.lexer.Print
import printscript.common.domain.RightParen
import printscript.common.domain.Semicolon
import printscript.common.domain.StringLiteral
import printscript.common.domain.Token
import printscript.common.domain.TokenType
import printscript.common.domain.Type
import java.util.Optional

/**
 * Builds tokens for parser unit tests without going through the lexer.
 */
object TokenFactory {
    private var col: Int = 1

    fun reset() {
        col = 1
    }

    fun let(): Token = tok(Let())
    fun colon(): Token = tok(Colon())
    fun assign(): Token = tok(Assign())
    fun semicolon(): Token = tok(Semicolon())
    fun lparen(): Token = tok(LeftParen())
    fun rparen(): Token = tok(RightParen())
    fun eof(): Token = tok(Eof())
    fun print(): Token = tok(Print())

    fun id(name: String): Token = tok(Identifier(), name)
    fun number(value: String): Token = tok(NumberLiteral(), value)
    fun string(value: String): Token = tok(StringLiteral(), value)
    fun type(name: String): Token = tok(Type(), name)
    fun op(symbol: String): Token = tok(Operator(), symbol)

    /** Sequence helper: builds tokens and appends EOF. */
    fun program(vararg tokens: Token): List<Token> = tokens.toList() + eof()

    private fun tok(type: TokenType, value: String? = null): Token {
        val start = CharPosition(1, col)
        val length = value?.length ?: 1
        col += length
        val end = CharPosition(1, col)
        col += 1
        return Token(type, Optional.ofNullable(value), start, end)
    }
}
