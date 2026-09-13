package printscript

import kotlin.test.Test
import kotlin.test.assertEquals

class SideEffectTest {
    @Test
    fun `PrintEffect carries the printed text`() {
        val effect = PrintEffect("hello")

        assertEquals("hello", effect.text)
    }
}
