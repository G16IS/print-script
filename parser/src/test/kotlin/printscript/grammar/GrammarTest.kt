package printscript.grammar

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.support.grammar
import printscript.support.or
import printscript.support.repeat

class GrammarTest {
    @Test
    fun `rejects an unknown start rule`() {
        assertThrows<IllegalArgumentException> {
            Grammar("missing", mapOf("n" to AtomRule("X")))
        }
    }

    @Test
    fun `rejects a reference to an unknown rule`() {
        assertThrows<IllegalArgumentException> {
            grammar("s", "s" to or("nope"))
        }
    }

    @Test
    fun `rejects a repeat of an unknown rule`() {
        assertThrows<IllegalArgumentException> {
            grammar("block", "block" to repeat("statement"))
        }
    }
}
