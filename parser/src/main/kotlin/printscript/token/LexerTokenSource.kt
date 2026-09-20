package printscript.token

import java.rmi.UnexpectedException
import printscript.Lexer
import printscript.domain.Token
import printscript.error.Error
import printscript.util.Result

class LexerTokenSource(
    private val lexer: Lexer,
) : TokenSource {
    private val buffer = mutableListOf<Token>()
    private var index = 0

    internal val retainedCount: Int
        get() = buffer.size

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

    override fun releaseConsumed() {
        if (index <= 0) return
        if (index >= buffer.size) {
            buffer.clear()
            index = 0
            return
        }
        buffer.subList(0, index).clear()
        index = 0
    }

    private fun fillTo(target: Int) {
        while (buffer.lastIndex < target) {
            val nextToken: Result<Token, Error> = lexer.nextToken()
            when (nextToken) {
                is Result.Err -> throw UnexpectedException("Could not read next token")
                is Result.Ok -> buffer += nextToken.value
            }
        }
    }

    private companion object {
        const val EOF = "EOF"
    }
}
