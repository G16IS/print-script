package printscript.support

import java.util.Optional
import printscript.Lexer
import printscript.domain.Token
import printscript.error.LexerError
import printscript.syntax.Location
import printscript.util.Result

class MockLexer(
    tokens: List<Token>,
) : Lexer {
    private val tokens: List<Token> = ensureEof(tokens)
    private var index: Int = 0

    override fun nextToken(): Result<Token, LexerError> {
        val token = tokens[index]
        if (index < tokens.lastIndex) index += 1
        return Result.Ok(token)
    }

    override fun peek(offset: Int): Result<Token, LexerError> {
        val i = index + offset
        return Result.Ok(tokens[i.coerceIn(0, tokens.lastIndex)])
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
