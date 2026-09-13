package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterLanguageConfig
import printscript.domain.LanguageFormatRuleSpec
import printscript.error.InvalidRuleParams
import printscript.error.UnknownRuleType
import printscript.formatter.config.FormatRuleLoader
import printscript.formatter.factories.FormatRuleFactories
import printscript.formatter.factories.NewlineRuleFactory
import printscript.formatter.factories.ResolvedFormatRule
import printscript.formatter.factories.SpaceRuleFactory
import printscript.formatter.rules.TokenNewlineRule
import printscript.formatter.rules.TokenSpaceRule
import printscript.util.Result

class FormatRuleFactoryTest {
    @Test
    fun `space-before space-after and space-around create rules`() {
        val before = SpaceRuleFactory.create(ResolvedFormatRule(type = "space-before", token = "COLON"))
        val after = SpaceRuleFactory.create(ResolvedFormatRule(type = "space-after", token = "COLON"))
        val around = SpaceRuleFactory.create(ResolvedFormatRule(type = "space-around", token = "ASSIGN"))

        assertTrue(before is Result.Ok && before.value is TokenSpaceRule)
        assertTrue(after is Result.Ok && after.value is TokenSpaceRule)
        assertTrue(around is Result.Ok && around.value is TokenSpaceRule)
    }

    @Test
    fun `space rule rejects a blank token`() {
        val result = SpaceRuleFactory.create(ResolvedFormatRule(type = "space-before", token = " "))

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is InvalidRuleParams)
    }

    @Test
    fun `space rule rejects an unknown type`() {
        val result = SpaceRuleFactory.create(ResolvedFormatRule(type = "space-inside", token = "COLON"))

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is InvalidRuleParams)
    }

    @Test
    fun `newline-before and newline-after create rules`() {
        val before = NewlineRuleFactory.create(ResolvedFormatRule(type = "newline-before", token = "CALL"))
        val after = NewlineRuleFactory.create(ResolvedFormatRule(type = "newline-after", token = "SEMICOLON"))

        assertTrue(before is Result.Ok && before.value is TokenNewlineRule)
        assertTrue(after is Result.Ok && after.value is TokenNewlineRule)
    }

    @Test
    fun `newline rule rejects a blank token`() {
        val result = NewlineRuleFactory.create(ResolvedFormatRule(type = "newline-after", token = ""))

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is InvalidRuleParams)
    }

    @Test
    fun `newline rule rejects an unknown type`() {
        val result = NewlineRuleFactory.create(ResolvedFormatRule(type = "newline-around", token = "CALL"))

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is InvalidRuleParams)
    }

    @Test
    fun `newline count zero is allowed`() {
        val result =
            NewlineRuleFactory.create(
                ResolvedFormatRule(type = "newline-after", token = "SEMICOLON", count = 0),
            )

        assertTrue(result is Result.Ok)
    }

    @Test
    fun `language rule with unknown type is a config error`() {
        val loader = FormatRuleLoader(FormatRuleFactories.defaults())
        val language =
            FormatterLanguageConfig(
                rules = listOf(LanguageFormatRuleSpec(type = "indent", token = "LET")),
            )
        val result = loader.load(language)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UnknownRuleType)
    }

    @Test
    fun `disabled space rule still instantiates`() {
        val result =
            SpaceRuleFactory.create(
                ResolvedFormatRule(type = "space-before", token = "COLON", enabled = false),
            )

        assertTrue(result is Result.Ok)
    }

    @Test
    fun `user extras that are not bindings stay unknown`() {
        val loader = FormatRuleLoader(FormatRuleFactories.defaults())
        val result =
            loader.load(
                FormatterLanguageConfig(),
                user = printscript.domain.FormatterRulesConfig(listOf(FormatRuleSpec(type = "space-before"))),
            )

        assertTrue(result is Result.Err)
        assertEquals("space-before", ((result as Result.Err).error as UnknownRuleType).type)
    }
}
