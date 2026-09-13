package printscript.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GrammarTest {
    @Test
    fun `accepts a grammar whose start and references exist`() {
        val grammar = validGrammar()

        assertEquals("s", grammar.start)
        assertTrue(grammar.rule("s") is OrRule)
        assertTrue(grammar.rule("atom") is AtomRule)
    }

    @Test
    fun `rejects an unknown start rule`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Grammar("missing", mapOf("n" to AtomRule("X")))
            }

        assertTrue(error.message!!.contains("Unknown start rule: missing"))
    }

    @Test
    fun `rejects a reference to an unknown rule`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Grammar(
                    start = "s",
                    rules = mapOf("s" to OrRule(listOf("nope"))),
                )
            }

        assertTrue(error.message!!.contains("Unknown rule references"))
        assertTrue(error.message!!.contains("nope"))
    }

    @Test
    fun `rejects a repeat of an unknown rule`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Grammar(
                    start = "block",
                    rules = mapOf("block" to RepeatRule("statement")),
                )
            }

        assertTrue(error.message!!.contains("statement"))
    }

    @Test
    fun `rejects a left rule whose operand rule is unknown`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Grammar(
                    start = "expr",
                    rules =
                        mapOf(
                            "expr" to LeftRule("num", OperatorSpec("OPERATOR", listOf("+"))),
                        ),
                )
            }

        assertTrue(error.message!!.contains("num"))
    }

    @Test
    fun `lists a missing seq rule-ref only once even if two steps share it`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Grammar(
                    start = "s",
                    rules =
                        mapOf(
                            "s" to
                                SeqRule(
                                    listOf(RuleRefStep("gone"), RuleRefStep("gone")),
                                ),
                        ),
                )
            }

        val message = error.message!!
        assertTrue(message.contains("gone"))
        assertEquals(1, Regex("\\bgone\\b").findAll(message).count())
    }

    @Test
    fun `rule returns the named production`() {
        val grammar = validGrammar()

        assertEquals(AtomRule("ID"), grammar.rule("atom"))
    }

    @Test
    fun `rule throws when the name is unknown`() {
        val error =
            assertFailsWith<IllegalStateException> {
                validGrammar().rule("nope")
            }

        assertTrue(error.message!!.contains("Unknown grammar rule: nope"))
    }

    @Test
    fun `AtomRule has no references`() {
        assertEquals(emptyList(), AtomRule("ID").references())
    }

    @Test
    fun `OrRule references its alternatives`() {
        assertEquals(listOf("a", "b"), OrRule(listOf("a", "b")).references())
    }

    @Test
    fun `SeqRule references only RuleRefSteps`() {
        val rule =
            SeqRule(
                listOf(
                    TokenStep("LET", capture = false),
                    RuleRefStep("expression"),
                    TokenStep("ID", capture = true),
                ),
            )

        assertEquals(listOf("expression"), rule.references())
    }

    @Test
    fun `TokenStep ruleName is null`() {
        assertNull(TokenStep("LET", capture = false).ruleName())
    }

    @Test
    fun `RuleRefStep ruleName is the rule name`() {
        assertEquals("expression", RuleRefStep("expression").ruleName())
    }

    @Test
    fun `LeftRule references its left operand`() {
        assertEquals(
            listOf("term"),
            LeftRule("term", OperatorSpec("OPERATOR", listOf("+"))).references(),
        )
    }

    @Test
    fun `RepeatRule references its item`() {
        assertEquals(listOf("statement"), RepeatRule("statement").references())
    }

    private fun validGrammar(): Grammar =
        Grammar(
            start = "s",
            rules =
                mapOf(
                    "s" to OrRule(listOf("seq", "atom")),
                    "seq" to
                        SeqRule(
                            listOf(
                                TokenStep(type = "LET", capture = false),
                                RuleRefStep("atom"),
                            ),
                        ),
                    "atom" to AtomRule("ID"),
                    "expr" to LeftRule("atom", OperatorSpec("OPERATOR", listOf("+", "-"))),
                    "many" to RepeatRule("atom"),
                ),
        )
}
