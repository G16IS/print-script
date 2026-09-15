package printscript.serializer.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.AtomRule
import printscript.domain.Grammar
import printscript.domain.GrammarRule
import printscript.domain.LeftRule
import printscript.domain.OperatorSpec
import printscript.domain.OptionalRule
import printscript.domain.OrRule
import printscript.domain.RepeatRule
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.TokenStep

class GrammarRuleSerializerTest {
    @Test
    fun `repeat rule round-trips`() {
        val rule = RepeatRule("item")
        assertEquals(rule, roundTrip(RepeatRuleSerializer, rule))
        assertEquals(rule, decode(GrammarRuleSerializer, """{"repeat":"item"}"""))
    }

    @Test
    fun `optional rule round-trips`() {
        val rule = OptionalRule("item")
        assertEquals(rule, roundTrip(OptionalRuleSerializer, rule))
        assertEquals(rule, decode(GrammarRuleSerializer, """{"optional":"item"}"""))
    }

    @Test
    fun `each grammar rule shape round-trips through GrammarRuleSerializer`() {
        val orRule = OrRule(listOf("a", "b"))
        val atom = AtomRule("ID")
        val seq = SeqRule(listOf(TokenStep("LET", false), RuleRefStep("expression")))
        val left = LeftRule("term", OperatorSpec("OPERATOR", listOf("+", "-")))
        val repeat = RepeatRule("statement")
        val optional = OptionalRule("statement")

        assertEquals(orRule, roundTrip(GrammarRuleSerializer, orRule))
        assertEquals(atom, roundTrip(GrammarRuleSerializer, atom))
        assertEquals(seq, roundTrip(GrammarRuleSerializer, seq))
        assertEquals(left, roundTrip(GrammarRuleSerializer, left))
        assertEquals(repeat, roundTrip(GrammarRuleSerializer, repeat))
        assertEquals(optional, roundTrip(GrammarRuleSerializer, optional))
    }

    @Test
    fun `repeat inside a valid grammar round-trips`() {
        val grammar =
            Grammar(
                start = "block",
                rules =
                    mapOf(
                        "block" to RepeatRule("item"),
                        "item" to AtomRule("ID"),
                    ),
            )
        assertEquals(grammar, roundTrip(GrammarSerializer, grammar))
    }

    @Test
    fun `rejects unknown grammar rule keys`() {
        val error =
            assertThrows<IllegalStateException> {
                decode(GrammarRuleSerializer, """{"maybe":[]}""")
            }
        assertTrue(error.message!!.contains("Unknown grammar rule keys"))
    }

    @Test
    fun `rejects serializing an unknown grammar rule implementation`() {
        val error =
            assertThrows<IllegalStateException> {
                serializerJson.encodeToString(GrammarRuleSerializer, FakeRule)
            }
        assertTrue(error.message!!.contains("Unknown grammar rule"))
    }

    private object FakeRule : GrammarRule {
        override fun references(): List<String> = emptyList()
    }
}
