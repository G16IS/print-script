package printscript.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.NodeConfig
import printscript.domain.Operation

class JSONTypeSystemConfigReaderTest {
    private val config = JSONTypeSystemConfigReader.read(resource())

    @Test
    fun `reads types from type system config`() {
        assertEquals(listOf("number", "string"), config.types)
    }

    @Test
    fun `reads literals mapped to types`() {
        assertEquals("number", config.literals["NUMBER_LITERAL"])
        assertEquals("string", config.literals["STRING_LITERAL"])
    }

    @Test
    fun `reads operations in order with commutative default`() {
        assertEquals(expectedOperations(), config.operations)
        assertTrue(config.operations.all { it.commutative })
    }

    @Test
    fun `reads declaration node child names`() {
        val variable = config.nodes.getValue("variable")
        assertEquals("declaration", variable.kind)
        assertEquals("ID", variable.id)
        assertEquals("TYPE", variable.declaredType)
        assertEquals("expression", variable.expression)
    }

    @Test
    fun `reads call node callee and args`() {
        val call = config.nodes.getValue("call")
        assertEquals("call", call.kind)
        assertEquals("CALL", call.callee)
        assertEquals(listOf("expression"), call.args)
    }

    @Test
    fun `reads nodes that only declare a kind`() {
        assertEquals(NodeConfig(kind = "binary-or-primary"), config.nodes.getValue("expression"))
        assertEquals(NodeConfig(kind = "binary-or-primary"), config.nodes.getValue("term"))
        assertEquals(NodeConfig(kind = "primary"), config.nodes.getValue("factor"))
        assertEquals(NodeConfig(kind = "literal"), config.nodes.getValue("number"))
        assertEquals(NodeConfig(kind = "literal"), config.nodes.getValue("string"))
        assertEquals(NodeConfig(kind = "identifier"), config.nodes.getValue("identifier"))
        assertNull(config.nodes.getValue("identifier").id)
    }

    @Test
    fun `reads commutative false when present`() {
        val loaded =
            JSONTypeSystemConfigReader.read(
                """
                {
                  "types": ["number"],
                  "operations": [
                    {
                      "op": "-",
                      "operands": ["number", "number"],
                      "result": "number",
                      "commutative": false
                    }
                  ]
                }
                """.trimIndent(),
            )

        assertEquals(false, loaded.operations.single().commutative)
    }

    @Test
    fun `rejects a literal that references an unknown type`() {
        val error =
            assertThrows<IllegalArgumentException> {
                JSONTypeSystemConfigReader.read(
                    """{"types":["number"],"literals":{"TRUE":"boolean"}}""",
                )
            }

        assertTrue(error.message!!.contains("Unknown types in literals"))
        assertTrue(error.message!!.contains("boolean"))
    }

    @Test
    fun `rejects an operation that references an unknown type`() {
        val error =
            assertThrows<IllegalArgumentException> {
                JSONTypeSystemConfigReader.read(
                    """
                    {
                      "types": ["number"],
                      "operations": [
                        { "op": "+", "operands": ["number", "string"], "result": "number" }
                      ]
                    }
                    """.trimIndent(),
                )
            }

        assertTrue(error.message!!.contains("Unknown types in operations"))
        assertTrue(error.message!!.contains("string"))
    }

    private fun expectedOperations() =
        listOf(
            Operation("+", listOf("number", "number"), "number"),
            Operation("+", listOf("string", "string"), "string"),
            Operation("+", listOf("string", "number"), "string"),
            Operation("-", listOf("number", "number"), "number"),
            Operation("*", listOf("number", "number"), "number"),
            Operation("/", listOf("number", "number"), "number"),
        )

    private fun resource() =
        checkNotNull(javaClass.getResourceAsStream("/type-system.config.v1.0.json")) {
            "Missing type-system.config.v1.0.json"
        }
}
