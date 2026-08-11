package printscript.token

import printscript.domain.Token
import printscript.domain.TokenType

/**
 * Cursor over a token stream. Keeps statement/expression parsers decoupled from
 * how tokens were produced (list, lexer, etc.).
 */
interface TokenSource {
    fun peek(): Token

    fun peek(offset: Int): Token

    fun advance(): Token

    fun check(predicate: (TokenType) -> Boolean): Boolean

    fun match(predicate: (TokenType) -> Boolean): Boolean

    fun expect(predicate: (TokenType) -> Boolean, message: String): Token

    fun isAtEnd(): Boolean
}
