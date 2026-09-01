package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.ExactRule
import printscript.domain.LanguageConfig
import printscript.domain.RegexRule
import printscript.domain.TokenRule
import printscript.support.PrintScriptLanguage

class RuleDrawResolverTest {
    private val let = ExactRule(listOf("let"), "LET", false)
    private val printlnRule = ExactRule(listOf("println"), "CALL", true)
    private val plus = ExactRule(listOf("+"), "OPERATOR", true)
    private val assign = ExactRule(listOf("="), "ASSIGN", false)
    private val type = ExactRule(listOf("string", "number"), "TYPE", true)
    private val identifier =
        RegexRule(
            matcher = listOf("^[a-zA-Z_][a-zA-Z0-9_]*"),
            token = "ID",
            capture = true,
            partial = "^[a-zA-Z_]",
        )
    private val number =
        RegexRule(
            matcher = listOf("^[0-9]+"),
            token = "NUMBER_LITERAL",
            capture = true,
            partial = "^[0-9]",
        )

    private fun languageConfig(
        order: List<String> = listOf("keywords", "operators", "literals", "identifiers"),
        config: Map<String, List<TokenRule>> =
            mapOf(
                "keywords" to listOf(let, printlnRule),
                "operators" to listOf(plus, assign),
                "literals" to listOf(number),
                "identifiers" to listOf(identifier),
                "types" to listOf(type),
            ),
    ): LanguageConfig = LanguageConfig(order, config)

    private fun resolver(langConfig: LanguageConfig = languageConfig()) = RuleDrawResolver(langConfig)

    @Nested
    inner class UniqueMatch {
        @Test
        fun `returns the only matching exact rule`() {
            assertEquals(let, resolver().resolve(listOf(let)))
        }

        @Test
        fun `returns the only matching regex rule`() {
            assertEquals(identifier, resolver().resolve(listOf(identifier)))
        }
    }

    @Nested
    inner class FirstInOrderWins {
        @Test
        fun `prefers the category that appears first in order`() {
            assertEquals(let, resolver().resolve(listOf(let, plus, identifier)))
        }

        @Test
        fun `prefers an earlier category over a later one`() {
            assertEquals(let, resolver().resolve(listOf(let, plus)))
        }

        @Test
        fun `picks the highest priority even when lower categories appear first in the list`() {
            assertEquals(let, resolver().resolve(listOf(identifier, number, let)))
        }

        @Test
        fun `ignores extra rules from lower priority categories`() {
            assertEquals(printlnRule, resolver().resolve(listOf(identifier, plus, printlnRule)))
        }

        @Test
        fun `a category missing from order is the lowest priority`() {
            val config = languageConfig(order = listOf("keywords", "operators"))
            assertEquals(let, resolver(config).resolve(listOf(identifier, let)))
        }

        @Test
        fun `list order of matching rules does not override category order`() {
            val keywordsFirst = languageConfig(order = listOf("keywords", "identifiers"))
            assertEquals(let, resolver(keywordsFirst).resolve(listOf(identifier, let)))
            assertEquals(let, resolver(keywordsFirst).resolve(listOf(let, identifier)))

            val identifiersFirst = languageConfig(order = listOf("identifiers", "keywords"))
            assertEquals(identifier, resolver(identifiersFirst).resolve(listOf(let, identifier)))
        }
    }

    @Nested
    inner class PrintScriptPriority {
        private val printScript = RuleDrawResolver(PrintScriptLanguage.config())
        private val reversed = RuleDrawResolver(PrintScriptLanguage.reversedOrder())
        private val rules = PrintScriptLanguage.config().config
        private val letRule = rules.getValue("keywords")[0]
        private val idRule = rules.getValue("identifiers")[0]
        private val typeRule = rules.getValue("types")[0]
        private val plusRule = rules.getValue("operators").first { it.token == "OPERATOR" }

        @Test
        fun `keywords first - let beats identifier`() {
            assertEquals(letRule, printScript.resolve(listOf(letRule, idRule)))
        }

        @Test
        fun `reversed order - identifier beats let`() {
            assertEquals(idRule, reversed.resolve(listOf(letRule, idRule)))
        }

        @Test
        fun `types beat identifiers for string`() {
            assertEquals(typeRule, printScript.resolve(listOf(typeRule, idRule)))
        }

        @Test
        fun `operators beat identifiers`() {
            assertEquals(plusRule, printScript.resolve(listOf(plusRule, idRule)))
        }

        @Test
        fun `keywords beat types`() {
            assertEquals(letRule, printScript.resolve(listOf(letRule, typeRule)))
        }
    }

    @Nested
    inner class Errors {
        @Test
        fun `empty matching list`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    resolver().resolve(emptyList())
                }
            assertEquals("No matching rules provided", exception.message)
        }

        @Test
        fun `two rules in the highest category`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    resolver().resolve(listOf(let, printlnRule))
                }
            assertEquals(
                "Multiple rules found with the same priority: ${listOf(let, printlnRule)}",
                exception.message,
            )
        }

        @Test
        fun `highest category has more than one match among mixed rules`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    resolver().resolve(listOf(let, printlnRule, plus))
                }
            assertEquals(
                "Multiple rules found with the same priority: ${listOf(let, printlnRule)}",
                exception.message,
            )
        }

        @Test
        fun `rule not present in the language config`() {
            val unknown = ExactRule(listOf("unknown"), "UNKNOWN", false)
            val exception =
                assertThrows<IllegalStateException> {
                    resolver().resolve(listOf(unknown))
                }
            assertEquals("Rule $unknown not found in config", exception.message)
        }

        @Test
        fun `one of several rules is not present in the language config`() {
            val unknown = ExactRule(listOf("unknown"), "UNKNOWN", false)
            val exception =
                assertThrows<IllegalStateException> {
                    resolver().resolve(listOf(let, unknown))
                }
            assertEquals("Rule $unknown not found in config", exception.message)
        }
    }
}
