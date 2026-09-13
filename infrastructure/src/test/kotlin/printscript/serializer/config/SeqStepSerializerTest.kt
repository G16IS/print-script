package printscript.serializer.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.RuleRefStep
import printscript.domain.SeqStep
import printscript.domain.TokenStep

class SeqStepSerializerTest {
    @Test
    fun `plain string is a non-capture token step`() {
        assertEquals(TokenStep("LET", capture = false), decode(SeqStepSerializer, """"LET""""))
    }

    @Test
    fun `capture object is a captured token step`() {
        assertEquals(TokenStep("ID", capture = true), decode(SeqStepSerializer, """{"capture":"ID"}"""))
    }

    @Test
    fun `rule object is a rule ref step`() {
        assertEquals(RuleRefStep("expression"), decode(SeqStepSerializer, """{"rule":"expression"}"""))
    }

    @Test
    fun `token and rule steps round-trip to the original json shape`() {
        assertEquals(TokenStep("LET", false), roundTrip(SeqStepSerializer, TokenStep("LET", false)))
        assertEquals(TokenStep("ID", true), roundTrip(SeqStepSerializer, TokenStep("ID", true)))
        assertEquals(RuleRefStep("expression"), roundTrip(SeqStepSerializer, RuleRefStep("expression")))
    }

    @Test
    fun `rejects an array step`() {
        val error =
            assertThrows<IllegalStateException> {
                decode(SeqStepSerializer, """["LET"]""")
            }
        assertTrue(error.message!!.contains("Invalid seq step"))
    }

    @Test
    fun `rejects an object without capture or rule`() {
        val error =
            assertThrows<IllegalStateException> {
                decode(SeqStepSerializer, """{"token":"LET"}""")
            }
        assertTrue(error.message!!.contains("Invalid seq step object"))
    }

    @Test
    fun `rejects serializing an unknown seq step implementation`() {
        val error =
            assertThrows<IllegalStateException> {
                serializerJson.encodeToString(SeqStepSerializer, FakeStep)
            }
        assertTrue(error.message!!.contains("Unknown seq step"))
    }

    private object FakeStep : SeqStep
}
