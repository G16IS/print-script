package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GapTest {
    @Test
    fun `space plus space stays one space`() {
        val gap = Gap(spaces = 1) + Gap(spaces = 1)

        assertEquals(Gap(newlines = 0, spaces = 1), gap)
        assertEquals(" ", gap.render(0))
    }

    @Test
    fun `newlines add so println extras survive`() {
        val gap = Gap(newlines = 1) + Gap(newlines = 1)

        assertEquals("\n\n", gap.render(0))
    }

    @Test
    fun `newlines drop intra-line space and indent at level 0 is identity`() {
        val gap = Gap(newlines = 1, spaces = 1)

        assertEquals("\n", gap.render(0))
    }

    @Test
    fun `newlines at indent 1 emit four spaces after the break`() {
        val gap = Gap(newlines = 1)

        assertEquals("\n    ", gap.render(1))
    }
}
