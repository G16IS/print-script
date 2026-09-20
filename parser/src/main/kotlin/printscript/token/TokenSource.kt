package printscript.token

import printscript.domain.Token

/**
 * Cursor over a token stream with speculative checkpoints.
 */
interface TokenSource {
    fun peek(): Token

    fun peek(offset: Int): Token

    fun advance(): Token

    fun isAtEnd(): Boolean

    fun checkpoint(): Int

    fun restore(mark: Int)

    fun releaseConsumed() {}
}
