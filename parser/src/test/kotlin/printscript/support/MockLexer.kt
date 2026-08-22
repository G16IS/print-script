package printscript.support

import java.util.Optional
import printscript.Lexer
import printscript.ast.Location
import printscript.domain.Token

class MockLexer(
    tokens: List<Token>,
) : Lexer {
    private val tokens: List<Token> = ensureEof(tokens)
    private var index: Int = 0

    override fun nextToken(): Token {
        val token = tokens[index]
        if (index < tokens.lastIndex) index += 1
        return token
    }

    override fun peek(offset: Int?): Token {
        val i = index + (offset ?: 0)
        return tokens[i.coerceIn(0, tokens.lastIndex)]
    }

    companion object {
        fun of(vararg tokens: Token): MockLexer = MockLexer(tokens.toList())

        private fun ensureEof(tokens: List<Token>): List<Token> {
            if (tokens.isNotEmpty() && tokens.last().type == "EOF") return tokens
            val location = tokens.lastOrNull()?.location ?: Location.empty()
            return tokens + Token("EOF", Optional.empty(), location)
        }
    }
}
