package printscript.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import printscript.domain.UserRuleBinding

class FormatterLanguageConfigReaderTest {
    @Test
    fun `reads language json resource`() {
        val config = JSONFormatterLanguageConfigReader.read(languageResource())

        assertEquals(
            listOf("space-around", "newline-after", "space-after"),
            config.rules.map { it.type },
        )
        assertEquals(listOf("OPERATOR", "SEMICOLON", "LET"), config.rules.map { it.token })
        assertEquals(
            listOf(
                "single-space-separation",
                "space-before-colon",
                "space-after-colon",
                "space-around-assign",
                "newlines-before-println",
            ),
            config.userBindings.map { it.userType },
        )
        val separation = config.userBindings.single { it.userType == "single-space-separation" }
        assertEquals("*", separation.token)
        assertEquals(false, separation.defaultEnabled)
        val println = config.userBindings.single { it.userType == "newlines-before-println" }
        assertEquals("CALL", println.token)
        assertEquals("println", println.value)
        assertEquals("SEMICOLON", println.previous)
        assertEquals(UserRuleBinding.PARAM_COUNT, println.param)
        assertEquals(1, println.defaultCount)
        assertNull(println.defaultEnabled)
        assertEquals(true, config.userBindings.single { it.userType == "space-before-colon" }.defaultEnabled)
    }

    private fun languageResource() =
        checkNotNull(javaClass.getResourceAsStream("/formatter-language.v1.0.json")) {
            "Missing formatter-language.v1.0.json"
        }
}
