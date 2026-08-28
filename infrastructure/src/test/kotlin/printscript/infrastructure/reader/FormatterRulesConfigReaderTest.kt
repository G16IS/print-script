package printscript.infrastructure.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import printscript.domain.FormatRuleSpec

class FormatterRulesConfigReaderTest {
    @Test
    fun `reads language json resource`() {
        val config = JSONFormatterRulesConfigReader.read(languageResource())

        assertEquals(listOf(FormatRuleSpec(type = "space-around-operator")), config.rules)
    }

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
    fun `json unknown keys are ignored`() {
        val config =
            JSONFormatterRulesConfigReader.read(
                """{"rules":[{"type":"space-around-operator","extra":true}]}""",
            )

        assertEquals("space-around-operator", config.rules.single().type)
        assertNull(config.rules.single().enabled)
    }

    private fun languageResource() =
        checkNotNull(javaClass.getResourceAsStream("/formatter-language.json")) {
            "Missing formatter-language.json"
        }
}
