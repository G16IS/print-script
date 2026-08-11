package printscript.support

import printscript.common.domain.Token
import printscript.common.domain.TokenType
import printscript.common.reader.CharPosition
import printscript.Lexer
import java.util.Optional

/**
 * Test double for [Lexer]. Yields tokens from a fixed list in order.
 * Appends a synthetic EOF if the list does not already end with one.
 * Once positioned on EOF, [nextToken] and [peek] remain stable.
 */
class MockLexer(tokens: List<Token>) : Lexer {
    private val tokens: List<Token> = ensureEof(tokens)
    private var index: Int = 0

    override fun nextToken(): Token {
        val token = tokens[index]
        if (index < tokens.lastIndex) {
            index++
        }
        return token
    }

    override fun peek(offset: Int?): Token {
        val i = index + (offset ?: 0)
        return tokens[i.coerceIn(0, tokens.lastIndex)]
    }

    companion object {
        fun of(vararg tokens: Token): MockLexer = MockLexer(tokens.toList())

        private fun ensureEof(tokens: List<Token>): List<Token> {
            if (tokens.isNotEmpty() && tokens.last().type == TokenType.EOF) {
                return tokens
            }
            val pos = tokens.lastOrNull()?.end ?: CharPosition(1, 1)
            return tokens + Token(TokenType.EOF, Optional.empty(), pos, pos)
        }
    }
}
