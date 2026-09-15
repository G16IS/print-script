package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterLanguageConfig
import printscript.domain.FormatterRulesConfig
import printscript.domain.TokenLexemes
import printscript.error.InvalidRuleParams
import printscript.error.UnknownRuleType
import printscript.formatter.config.FormatRuleLoader
import printscript.formatter.factories.FormatRuleFactories
import printscript.formatter.support.spaceAroundOperator
import printscript.reader.JSONFormatterLanguageConfigReader
import printscript.reader.JSONFormatterRulesConfigReader
import printscript.reader.JSONGrammarConfigReader
import printscript.util.Result

class FormatRuleLoaderTest {
    private val loader = FormatRuleLoader(FormatRuleFactories.defaults())
    private val grammar =
        JSONGrammarConfigReader.read(
            checkNotNull(javaClass.getResourceAsStream("/grammar.config.v1.0.json")) {
                "Missing grammar.config.v1.0.json"
            },
        )
    private val lexemes =
        TokenLexemes(
            mapOf(
                "LET" to "let",
                "COLON" to ":",
                "ASSIGN" to "=",
                "SEMICOLON" to ";",
                "LEFT_PAREN" to "(",
                "RIGHT_PAREN" to ")",
            ),
        )
    private val language = JSONFormatterLanguageConfigReader.read(languageResource())
    private val defaults = JSONFormatterRulesConfigReader.read(defaultsResource())

    @Test
    fun `language json loads the built-in operator rule`() {
        val result = loader.load(language, defaults = defaults)

        assertTrue(result is Result.Ok)

        val rules = (result as Result.Ok).value

        assertTrue(rules.contains(spaceAroundOperator()))
        assertTrue(rules.size > 1)
    }

    @Test
    fun `empty user config still loads colon assign and println defaults`() {
        val result = loader.load(language, defaults = defaults)

        assertTrue(result is Result.Ok)
        assertEquals(8, (result as Result.Ok).value.size)
    }

    @Test
    fun `binding default applies when defaults json omits the rule`() {
        val result = loader.load(language)

        assertTrue(result is Result.Ok)
        assertEquals(8, (result as Result.Ok).value.size)
    }

    @Test
    fun `missing user config is not an error`() {
        val result =
            DefaultFormatterFactory.createFromConfig(
                language,
                defaults = defaults,
                grammar = grammar,
                lexemes = lexemes,
            )

        assertTrue(result is Result.Ok)
    }

    @Test
    fun `unknown type in user config is a config error`() {
        val user = FormatterRulesConfig(listOf(FormatRuleSpec(type = "not-a-real-rule")))
        val result = loader.load(FormatterLanguageConfig(), user)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UnknownRuleType)
    }

    @Test
    fun `user config cannot declare a language generic type`() {
        val user = FormatterRulesConfig(listOf(FormatRuleSpec(type = "space-around")))
        val result = loader.load(language, user, defaults)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UnknownRuleType)
    }

    @Test
    fun `println count outside allowed range is invalid`() {
        val user =
            FormatterRulesConfig(
                listOf(FormatRuleSpec(type = "newlines-before-println", count = 5)),
            )
        val result = loader.load(language, user, defaults)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is InvalidRuleParams)
    }

    private fun languageResource() =
        checkNotNull(javaClass.getResourceAsStream("/formatter-language.v1.0.json")) {
            "Missing formatter-language.v1.0.json"
        }

    private fun defaultsResource() =
        checkNotNull(javaClass.getResourceAsStream("/formatter-user-defaults.json")) {
            "Missing formatter-user-defaults.json"
        }
}
