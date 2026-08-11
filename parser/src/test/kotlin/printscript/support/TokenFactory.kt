package printscript.support

import printscript.common.domain.Token
import printscript.common.domain.TokenType
import printscript.common.reader.CharPosition
import java.util.Optional

/**
 * Builds tokens for parser unit tests without going through the real lexer.
 */
object TokenFactory {
    private var col: Int = 1

    fun reset() {
        col = 1
    }

    fun let(): Token = tok(TokenType.LET)
    fun colon(): Token = tok(TokenType.COLON)
    fun assign(): Token = tok(TokenType.ASSIGN)
    fun semicolon(): Token = tok(TokenType.SEMICOLON)
    fun lparen(): Token = tok(TokenType.LEFT_PAREN)
    fun rparen(): Token = tok(TokenType.RIGHT_PAREN)
    fun eof(): Token = tok(TokenType.EOF)
    fun print(): Token = tok(TokenType.CALL, "println")

    fun id(name: String): Token = tok(TokenType.IDENTIFIER, name)
    fun number(value: String): Token = tok(TokenType.NUMBER_LITERAL, value)
    fun string(value: String): Token = tok(TokenType.STRING_LITERAL, value)
    fun type(name: String): Token = tok(TokenType.TYPE, name)
    fun op(symbol: String): Token = tok(TokenType.OPERATOR, symbol)

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
