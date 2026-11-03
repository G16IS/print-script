package printscript.parser.token

import printscript.Lexer
import printscript.domain.Token
import printscript.error.LexerError
import printscript.util.fold

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
            lexer.nextToken().fold(
                onOk = { buffer += it },
                onErr = { throw LexerReadException(it) },
            )
        }
    }

    private companion object {
        const val EOF = "EOF"
    }
}

/**
 * El lexer falló mientras el parser miraba hacia adelante. Conserva el error original (mensaje y
 * posición) para que quien parsea pueda convertirlo en un resultado en vez de perderlo.
 */
class LexerReadException(
    val error: LexerError,
) : RuntimeException(error.message)
