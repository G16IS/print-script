package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.formatter.rules.SpaceAroundOperatorRule
import printscript.formatter.support.addition
import printscript.formatter.support.program

class FormatterCheckTest {
    private val formatter = DefaultFormatterFactory.create(listOf(SpaceAroundOperatorRule))

    @Test
    fun `reports both sides of an operator without spaces`() {
        val program = program(addition(leftCol = 1, opCol = 2, rightCol = 3))
        val report = formatter.check(program, "1+2")
        assertEquals(2, report.errors.size)
        assertTrue(report.errors.all { it is WhitespaceMismatch })
    }

    @Test
    fun `accepts spaces around the operator`() {
        val program = program(addition(leftCol = 1, opCol = 3, rightCol = 5))
        val report = formatter.check(program, "1 + 2")
        assertTrue(report.isOk)
        assertEquals(0, report.errors.size)
    }

    @Test
    fun `accumulates only the mismatched side and keeps going`() {
        val program = program(addition(leftCol = 1, opCol = 2, rightCol = 4))
        val report = formatter.check(program, "1+ 2")
        assertEquals(1, report.errors.size)
        val mismatch = report.errors.single() as WhitespaceMismatch
        assertEquals(" ", mismatch.expected)
        assertEquals("", mismatch.actual)
    }
}
