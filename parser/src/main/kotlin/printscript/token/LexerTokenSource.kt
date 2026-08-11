package printscript.token

import printscript.Lexer
import printscript.common.domain.Token
import printscript.common.domain.TokenType
import printscript.parser.error.ParseException
import printscript.parser.util.Locations
import java.util.Optional

data class LexerTokenSource(val tokens: Lexer) : TokenSource {
    override fun peek(): Token = tokens.peek(null)
    override fun peek(offset: Int): Token = tokens.peek(offset)

    override fun advance(): Token {
        val current = peek()

        if (!isAtEnd()) {
            return tokens.nextToken()
        }

        return current
    }

    override fun check(predicate: (TokenType) -> Boolean): Boolean = predicate(peek().type)

    override fun match(predicate: (TokenType) -> Boolean): Boolean {
        if (check(predicate)) {
            advance()
            return true
        }
        return false
    }

    override fun expect(predicate: (TokenType) -> Boolean, message: String): Token {
        val token = peek()
        if (!predicate(token.type)) {
            throw ParseException(message, Locations.of(token))
        }
        return advance()
    }

    override fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun syntheticEof(existing: List<Token>): Token {
        val pos = existing.lastOrNull()?.end
            ?: printscript.common.reader.CharPosition(1, 1)
        return Token(TokenType.EOF, Optional.empty(), pos, pos)
    }
}
