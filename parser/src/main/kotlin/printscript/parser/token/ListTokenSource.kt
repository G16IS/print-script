package printscript.parser.token

import printscript.lexer.Eof
import printscript.lexer.Token
import printscript.lexer.TokenType
import printscript.parser.error.ParseException
import printscript.parser.util.Locations
import java.util.Optional

class ListTokenSource(tokens: List<Token>) : TokenSource {
    private val tokens: List<Token> = if (tokens.isEmpty() || tokens.last().type !is Eof) {
        tokens + syntheticEof(tokens)
    } else {
        tokens
    }
    private var index: Int = 0

    override fun peek(): Token = tokens[index.coerceAtMost(tokens.lastIndex)]

    override fun peek(offset: Int): Token {
        val i = (index + offset).coerceIn(0, tokens.lastIndex)
        return tokens[i]
    }

    override fun advance(): Token {
        val current = peek()
        if (!isAtEnd()) {
            index++
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

    override fun isAtEnd(): Boolean = peek().type is Eof

    private fun syntheticEof(existing: List<Token>): Token {
        val pos = existing.lastOrNull()?.end
            ?: printscript.common.reader.CharPosition(1, 1)
        return Token(Eof(), Optional.empty(), pos, pos)
    }
}
