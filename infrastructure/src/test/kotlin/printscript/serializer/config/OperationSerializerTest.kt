package printscript.serializer.config

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import printscript.domain.Operation

class OperationSerializerTest {
    @Test
    fun `commutative false round-trips and is written to json`() {
        val operation = Operation("-", listOf("number", "number"), "number", commutative = false)

        val encoded = serializerJson.encodeToJsonElement(OperationSerializer, operation).jsonObject
        assertEquals("false", encoded.getValue("commutative").jsonPrimitive.content)
        assertEquals(operation, roundTrip(OperationSerializer, operation))
        assertFalse(roundTrip(OperationSerializer, operation).commutative)
    }

    @Test
    fun `commutative defaults to true when omitted`() {
        val operation =
            decode(
                OperationSerializer,
                """{"op":"+","operands":["number","number"],"result":"number"}""",
            )
        assertEquals(Operation("+", listOf("number", "number"), "number", commutative = true), operation)
    }
}
