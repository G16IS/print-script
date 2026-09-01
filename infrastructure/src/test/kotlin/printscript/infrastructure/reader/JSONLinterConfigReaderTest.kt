package printscript.infrastructure.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JSONLinterConfigReaderTest {
    private val config = JSONLinterConfigReader.read(resource())

    @Test
    fun `reads rules from linter config resource`() {
        assertEquals(2, config.rules.size)
        assertTrue(config.rules.containsKey("identifier-format"))
        assertTrue(config.rules.containsKey("println-simple-argument"))
    }

    @Test
    fun `reads enabled status and options`() {
        val idRule = config.rules.getValue("identifier-format")
        assertTrue(idRule.enabled)
        assertEquals("camelCase", idRule.options["format"])

        val printRule = config.rules.getValue("println-simple-argument")
        assertTrue(printRule.enabled)
        assertEquals("println", printRule.options["callee"])
    }

    @Test
    fun `filters enabled rules`() {
        val json =
            """
            {
              "rules": {
                "rule-a": { "enabled": true },
                "rule-b": { "enabled": false }
              }
            }
            """.trimIndent()

        val parsed = JSONLinterConfigReader.read(json)
        assertEquals(2, parsed.rules.size)
        val enabled = parsed.enabled()
        assertEquals(1, enabled.size)
        assertTrue(enabled.containsKey("rule-a"))
        assertFalse(enabled.containsKey("rule-b"))
    }

    private fun resource() =
        checkNotNull(javaClass.getResourceAsStream("/linter.config.json")) {
            "Missing linter.config.json"
        }
}
