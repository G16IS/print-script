package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterRulesConfig
import printscript.formatter.config.FormatRuleLoader
import printscript.formatter.factories.FormatRuleFactories
import printscript.formatter.rules.SpaceAroundOperatorRule
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.util.Result

class FormatRuleLoaderTest {
    private val loader = FormatRuleLoader(FormatRuleFactories.defaults())

    @Test
    fun `language json loads the built-in operator rule`() {
        val language = JSONFormatterRulesConfigReader.read(languageResource())
        val result = loader.load(language)

        assertTrue(result is Result.Ok)

        val rules = (result as Result.Ok).value

        assertTrue(rules.contains(SpaceAroundOperatorRule))
        assertTrue(rules.size > 1)
    }

    @Test
    fun `empty user config still loads colon assign and println defaults`() {
        val result = loader.load(FormatterRulesConfig())

        assertTrue(result is Result.Ok)
        assertEquals(4, (result as Result.Ok).value.size)
    }

    @Test
    fun `missing user config is not an error`() {
        val language = JSONFormatterRulesConfigReader.read(languageResource())
        val result = DefaultFormatterFactory.createFromConfig(language)

        assertTrue(result is Result.Ok)
    }

    @Test
    fun `unknown type in user config is a config error`() {
        val language = FormatterRulesConfig()
        val user = FormatterRulesConfig(listOf(FormatRuleSpec(type = "not-a-real-rule")))
        val result = loader.load(language, user)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UnknownRuleType)
    }

    @Test
    fun `user config cannot declare a language-fixed rule`() {
        val language = FormatterRulesConfig()
        val user = FormatterRulesConfig(listOf(FormatRuleSpec(type = "space-around-operator")))
        val result = loader.load(language, user)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UserDeclaredFixedRule)
    }

    @Test
    fun `println count outside allowed range is invalid`() {
        val user =
            FormatterRulesConfig(
                listOf(FormatRuleSpec(type = "newlines-before-println", count = 5)),
            )
        val result = loader.load(FormatterRulesConfig(), user)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is InvalidRuleParams)
    }

    private fun languageResource() =
        checkNotNull(javaClass.getResourceAsStream("/formatter-language.json")) {
            "Missing formatter-language.json"
        }
}
