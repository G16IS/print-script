package printscript.serializer.config

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.ExactRule
import printscript.domain.RegexRule

class TokenRuleSerializerTest {
    @Test
    fun `exact rule round-trips and keeps type`() {
        val rule = ExactRule(listOf("let"), "LET", capture = false)

        val encoded = serializerJson.encodeToJsonElement(TokenRuleSerializer, rule).jsonObject
        assertEquals("exact", encoded.getValue("type").jsonPrimitive.content)
        assertEquals(rule, roundTrip(TokenRuleSerializer, rule))
    }

    @Test
    fun `regex rule round-trips and keeps type`() {
        val rule =
            RegexRule(
                matcher = listOf("^[0-9]"),
                token = "NUMBER_LITERAL",
                capture = true,
                partial = "^[0-9]",
            )

        val encoded = serializerJson.encodeToJsonElement(TokenRuleSerializer, rule).jsonObject
        assertEquals("regex", encoded.getValue("type").jsonPrimitive.content)
        assertEquals(rule, roundTrip(TokenRuleSerializer, rule))
    }

    @Test
    fun `rejects a token rule without type`() {
        val error =
            assertThrows<IllegalStateException> {
                decode(TokenRuleSerializer, """{"matcher":["let"],"token":"LET","capture":false}""")
            }
        assertTrue(error.message!!.contains("TokenRule missing type"))
    }

    @Test
    fun `rejects an unknown token rule type`() {
        val error =
            assertThrows<IllegalStateException> {
                decode(
                    TokenRuleSerializer,
                    """{"type":"glob","matcher":["*"],"token":"X","capture":false}""",
                )
            }
        assertTrue(error.message!!.contains("Unknown token rule type"))
    }
}
