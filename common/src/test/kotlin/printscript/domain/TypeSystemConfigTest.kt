package printscript.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypeSystemConfigTest {
    @Test
    fun `accepts the canonical type system`() {
        val config = canonicalTypeSystem()

        assertEquals(listOf("number", "string"), config.types)
        assertEquals("number", config.literals["NUMBER_LITERAL"])
        assertEquals("string", config.literals["STRING_LITERAL"])
        assertEquals(6, config.operations.size)
        assertEquals("declaration", config.nodes.getValue("variable").kind)
        assertEquals("ID", config.nodes.getValue("variable").id)
        assertEquals("TYPE", config.nodes.getValue("variable").declaredType)
        assertEquals("expression", config.nodes.getValue("variable").expression)
        assertEquals(listOf("expression"), config.nodes.getValue("call").args)
        assertTrue(config.operations.first().commutative)
    }

    @Test
    fun `accepts a config that only declares types`() {
        val config = TypeSystemConfig(types = listOf("number"))

        assertEquals(listOf("number"), config.types)
        assertEquals(emptyMap(), config.literals)
        assertEquals(emptyList(), config.operations)
        assertEquals(emptyMap(), config.nodes)
    }

    @Test
    fun `rejects a literal that references an unknown type`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                TypeSystemConfig(
                    types = listOf("number"),
                    literals = mapOf("TRUE" to "boolean"),
                )
            }

        assertTrue(error.message!!.contains("Unknown types in literals"))
        assertTrue(error.message!!.contains("boolean"))
    }

    @Test
    fun `rejects an operation operand of an unknown type`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                TypeSystemConfig(
                    types = listOf("number"),
                    operations =
                        listOf(
                            Operation("+", listOf("number", "boolean"), "number"),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Unknown types in operations"))
        assertTrue(error.message!!.contains("boolean"))
    }

    @Test
    fun `rejects an operation result of an unknown type`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                TypeSystemConfig(
                    types = listOf("number"),
                    operations =
                        listOf(
                            Operation("+", listOf("number", "number"), "string"),
                        ),
                )
            }

        assertTrue(error.message!!.contains("Unknown types in operations"))
        assertTrue(error.message!!.contains("string"))
    }

    @Test
    fun `node fields not used by a kind stay absent`() {
        val literal = NodeConfig(kind = "literal")

        assertNull(literal.id)
        assertNull(literal.declaredType)
        assertNull(literal.expression)
        assertNull(literal.callee)
        assertEquals(emptyList(), literal.args)
    }

    @Test
    fun `keeps commutative false when it is set`() {
        val op = Operation("+", listOf("number", "number"), "number", commutative = false)

        val config = TypeSystemConfig(types = listOf("number"), operations = listOf(op))

        assertEquals(false, config.operations.single().commutative)
    }

    @Test
    fun `lists each unknown type only once across operations`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                TypeSystemConfig(
                    types = listOf("number"),
                    operations =
                        listOf(
                            Operation("+", listOf("boolean", "boolean"), "boolean"),
                        ),
                )
            }

        val message = error.message!!
        assertTrue(message.contains("Unknown types in operations"))
        assertEquals(1, Regex("boolean").findAll(message).count())
    }

    private fun canonicalTypeSystem(): TypeSystemConfig =
        TypeSystemConfig(
            types = listOf("number", "string"),
            literals =
                mapOf(
                    "NUMBER_LITERAL" to "number",
                    "STRING_LITERAL" to "string",
                ),
            operations =
                listOf(
                    Operation("+", listOf("number", "number"), "number"),
                    Operation("+", listOf("string", "string"), "string"),
                    Operation("+", listOf("string", "number"), "string"),
                    Operation("-", listOf("number", "number"), "number"),
                    Operation("*", listOf("number", "number"), "number"),
                    Operation("/", listOf("number", "number"), "number"),
                ),
            nodes =
                mapOf(
                    "variable" to
                        NodeConfig(
                            kind = "declaration",
                            id = "ID",
                            declaredType = "TYPE",
                            expression = "expression",
                        ),
                    "expression-stmt" to
                        NodeConfig(
                            kind = "expression",
                            expression = "expression",
                        ),
                    "expression" to NodeConfig(kind = "binary-or-primary"),
                    "term" to NodeConfig(kind = "binary-or-primary"),
                    "factor" to NodeConfig(kind = "primary"),
                    "call" to
                        NodeConfig(
                            kind = "call",
                            callee = "CALL",
                            args = listOf("expression"),
                        ),
                    "group" to
                        NodeConfig(
                            kind = "group",
                            expression = "expression",
                        ),
                    "number" to NodeConfig(kind = "literal"),
                    "string" to NodeConfig(kind = "literal"),
                    "identifier" to NodeConfig(kind = "identifier"),
                ),
        )
}
