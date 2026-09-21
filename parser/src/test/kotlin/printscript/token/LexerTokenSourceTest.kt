package printscript.token

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.parser.token.LexerTokenSource
import printscript.support.MockLexer
import printscript.support.Tokens

class LexerTokenSourceTest {
    @Test
    fun `releaseConsumed drops tokens already advanced and keeps lookahead`() {
        Tokens.reset()
        val source =
            LexerTokenSource(
                MockLexer((1..100).map { Tokens.number("$it") }),
            )

        repeat(80) { source.advance() }
        val lookahead = source.peek()
        source.releaseConsumed()

        assertEquals(lookahead.type, source.peek().type)
        assertEquals(lookahead.value, source.peek().value)
        assertTrue(source.retainedCount <= 8)
    }

    @Test
    fun `checkpoint restore still works when consumed tokens have not been released`() {
        Tokens.reset()
        val source =
            LexerTokenSource(
                MockLexer(listOf(Tokens.number("1"), Tokens.number("2"), Tokens.number("3"))),
            )

        source.advance()
        val mark = source.checkpoint()
        val afterMark = source.advance()
        source.restore(mark)

        assertEquals(afterMark.type, source.peek().type)
        assertEquals(afterMark.value, source.peek().value)
    }
}
