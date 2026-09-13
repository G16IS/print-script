package printscript.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LinterConfigTest {
    @Test
    fun `enabled keeps only rules that are on`() {
        val camel = RuleConfig(enabled = true, options = mapOf("format" to "camelCase"))
        val printlnRule = RuleConfig(enabled = false)

        val config =
            LinterConfig(
                rules =
                    mapOf(
                        "identifier-format" to camel,
                        "println-simple-argument" to printlnRule,
                    ),
            )

        assertEquals(mapOf("identifier-format" to camel), config.enabled())
    }

    @Test
    fun `enabled is empty when there are no rules`() {
        assertEquals(emptyMap(), LinterConfig().enabled())
    }
}
