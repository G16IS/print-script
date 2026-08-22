package printscript.infrastructure.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.AtomRule
import printscript.domain.LeftRule
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
        assertEquals(listOf("variable", "expression-stmt"), statement.alternatives)
    }

    @Test
    fun `reads seq steps including captures and rule refs`() {
        val variable = grammar.rules.getValue("variable") as SeqRule
        assertEquals(expectedVariableSteps(), variable.steps)
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
            TokenStep("ASSIGN", false),
            RuleRefStep("expression"),
            TokenStep("SEMICOLON", false),
        )

    private fun resource() =
        checkNotNull(javaClass.getResourceAsStream("/grammar.config.json")) {
            "Missing grammar.config.json"
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
}
