package printscript.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.AtomRule
import printscript.domain.LeftRule
import printscript.domain.OptionalRule
import printscript.domain.OrRule
import printscript.domain.RepeatRule
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.TokenStep

class JSONGrammarConfigReaderTest {
    private val grammar = JSONGrammarConfigReader.read(resource())

    @Test
    fun `reads start rule from grammar config`() {
        assertEquals("statement", grammar.start)
    }

    @Test
    fun `reads or rules`() {
        val statement = grammar.rules.getValue("statement") as OrRule
        assertEquals(listOf("variable", "expression-stmt", "assignment"), statement.alternatives)
    }

    @Test
    fun `reads seq steps including captures and rule refs`() {
        val variable = grammar.rules.getValue("variable") as SeqRule
        assertEquals(expectedVariableSteps(), variable.steps)
        val initializer = grammar.rules.getValue("initializer") as OptionalRule
        assertEquals("var-init", initializer.item)
    }

    @Test
    fun `reads left rules with operator values`() {
        val expression = grammar.rules.getValue("expression") as LeftRule
        assertEquals("term", expression.left)
        assertEquals("OPERATOR", expression.op.token)
        assertEquals(listOf("+", "-"), expression.op.values)
    }

    @Test
    fun `reads atom rules`() {
        val number = grammar.rules.getValue("number") as AtomRule
        assertEquals("NUMBER_LITERAL", number.token)
    }

    @Test
    fun `reads a repeat rule`() {
        val loaded = JSONGrammarConfigReader.read(repeatJson())
        val block = loaded.rules.getValue("block") as RepeatRule
        assertEquals("item", block.item)
    }

    @Test
    fun `reads an optional rule`() {
        val loaded = JSONGrammarConfigReader.read(optionalJson())
        val opt = loaded.rules.getValue("maybe") as OptionalRule
        assertEquals("item", opt.item)
    }

    @Test
    fun `rejects an unknown start rule`() {
        assertThrows<IllegalArgumentException> {
            JSONGrammarConfigReader.read("""{"start":"nope","rules":{"n":{"atom":"X"}}}""")
        }
    }

    @Test
    fun `rejects a reference to an unknown rule`() {
        assertThrows<IllegalArgumentException> {
            JSONGrammarConfigReader.read("""{"start":"s","rules":{"s":{"or":["nope"]}}}""")
        }
    }

    @Test
    fun `rejects an unknown rule shape`() {
        val error =
            assertThrows<IllegalStateException> {
                JSONGrammarConfigReader.read("""{"start":"s","rules":{"s":{"maybe":[]}}}""")
            }
        assertTrue(error.message!!.contains("Unknown grammar rule keys"))
    }

    private fun expectedVariableSteps() =
        listOf(
            TokenStep("LET", false),
            TokenStep("ID", true),
            TokenStep("COLON", false),
            TokenStep("TYPE", true),
            RuleRefStep("initializer"),
            TokenStep("SEMICOLON", false),
        )

    private fun resource() =
        checkNotNull(javaClass.getResourceAsStream("/grammar.config.v1.0.json")) {
            "Missing grammar.config.v1.0.json"
        }

    private fun repeatJson() =
        """
        {
          "start": "block",
          "rules": {
            "block": { "repeat": "item" },
            "item": { "atom": "ID" }
          }
        }
        """.trimIndent()

    private fun optionalJson() =
        """
        {
          "start": "maybe",
          "rules": {
            "maybe": { "optional": "item" },
            "item": { "atom": "ID" }
          }
        }
        """.trimIndent()
}
