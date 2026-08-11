package printscript.token

import printscript.Lexer
import printscript.domain.Token
import printscript.domain.TokenType
import printscript.error.ParseException
import printscript.util.Locations
import printscript.reader.CharPosition
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
            ?: CharPosition(1, 1)
        return Token(TokenType.EOF, Optional.empty(), pos, pos)
    }
}
