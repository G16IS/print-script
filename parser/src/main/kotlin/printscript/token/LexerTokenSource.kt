package printscript.token

import printscript.Lexer
import printscript.domain.Token

class LexerTokenSource(private val lexer: Lexer) : TokenSource {
    private val buffer = mutableListOf<Token>()
    private var index = 0

    override fun peek(): Token = peek(0)

    override fun peek(offset: Int): Token {
        fillTo(index + offset)
        return buffer[index + offset]
    }

    override fun advance(): Token {
        val current = peek()
        if (!isAtEnd()) index += 1
        return current
    }

    override fun isAtEnd(): Boolean = peek().type == EOF

    override fun checkpoint(): Int = index

    override fun restore(mark: Int) {
        index = mark
    }

    private fun fillTo(target: Int) {
        while (buffer.lastIndex < target) {
            buffer += lexer.nextToken()
        }
    }

    private companion object {
        const val EOF = "EOF"
    }
}
