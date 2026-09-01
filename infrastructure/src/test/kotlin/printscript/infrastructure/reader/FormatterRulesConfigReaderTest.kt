package printscript.infrastructure.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import printscript.domain.FormatRuleSpec

class FormatterRulesConfigReaderTest {
    @Test
    fun `reads user yaml with optional params`() {
        val config =
            YAMLFormatterRulesConfigReader.read(
                """
                rules:
                  - type: space-before-colon
                    enabled: true
                  - type: newlines-before-println
                    count: 2
                """.trimIndent(),
            )

        assertEquals(
            listOf(
                FormatRuleSpec(type = "space-before-colon", enabled = true),
                FormatRuleSpec(type = "newlines-before-println", count = 2),
            ),
            config.rules,
        )
    }

    @Test
    fun `empty yaml rules is valid`() {
        val config = YAMLFormatterRulesConfigReader.read("rules: []")

        assertEquals(emptyList<FormatRuleSpec>(), config.rules)
    }

    @Test
    fun `reads user defaults json resource`() {
        val config =
            JSONFormatterRulesConfigReader.read(
                checkNotNull(javaClass.getResourceAsStream("/formatter-user-defaults.json")) {
                    "Missing formatter-user-defaults.json"
                },
            )

        assertEquals(
            listOf(
                FormatRuleSpec(type = "space-before-colon", enabled = true),
                FormatRuleSpec(type = "space-after-colon", enabled = true),
                FormatRuleSpec(type = "space-around-assign", enabled = true),
                FormatRuleSpec(type = "newlines-before-println", count = 1),
            ),
            config.rules,
        )
    }

    @Test
    fun `json unknown keys are ignored`() {
        val config =
            JSONFormatterRulesConfigReader.read(
                """{"rules":[{"type":"space-before-colon","extra":true}]}""",
            )

        assertEquals("space-before-colon", config.rules.single().type)
        assertNull(config.rules.single().enabled)
    }
}
