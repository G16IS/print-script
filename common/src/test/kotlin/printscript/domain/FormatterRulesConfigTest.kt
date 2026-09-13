package printscript.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatterRulesConfigTest {
    @Test
    fun `defaults to no rules`() {
        assertEquals(emptyList(), FormatterRulesConfig().rules)
    }

    @Test
    fun `keeps optional enabled and count`() {
        val spec = FormatRuleSpec(type = "space-before-colon", enabled = true, count = null)

        assertEquals("space-before-colon", spec.type)
        assertEquals(true, spec.enabled)
        assertNull(spec.count)
    }
}
