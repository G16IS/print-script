package printscript.syntax

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import printscript.domain.Token
import printscript.support.loc

class SyntaxNodeTest {
    @Test
    fun `childOrNull returns the first direct child with that name`() {
        val id = leaf("ID", "a")
        val parent =
            SyntaxNode(
                name = "variable",
                children = listOf(id, leaf("TYPE", "number")),
                location = loc(),
            )

        assertSame(id, parent.childOrNull("ID"))
    }

    @Test
    fun `childOrNull is null when no direct child matches`() {
        val parent = SyntaxNode(name = "variable", children = listOf(leaf("ID")), location = loc())

        assertNull(parent.childOrNull("TYPE"))
    }

    @Test
    fun `child returns the named direct child`() {
        val id = leaf("ID", "a")
        val parent = SyntaxNode(name = "variable", children = listOf(id), location = loc())

        assertSame(id, parent.child("ID"))
    }

    @Test
    fun `child throws when the name is missing`() {
        val parent = SyntaxNode(name = "variable", children = emptyList(), location = loc())
        val error = assertFailsWith<IllegalStateException> { parent.child("ID") }

        assertTrue(error.message!!.contains("No child named 'ID' in 'variable'"))
    }

    @Test
    fun `findOrNull returns this when the name matches`() {
        val node = leaf("ID", "a")

        assertSame(node, node.findOrNull("ID"))
    }

    @Test
    fun `findOrNull walks descendants depth-first`() {
        val inner = leaf("NUMBER_LITERAL", "1")
        val term = SyntaxNode(name = "term", children = listOf(inner), location = loc())
        val expr = SyntaxNode(name = "expression", children = listOf(term), location = loc())

        assertSame(inner, expr.findOrNull("NUMBER_LITERAL"))
    }

    @Test
    fun `findOrNull is null when the name is absent`() {
        val node = SyntaxNode(name = "expression", children = listOf(leaf("ID")), location = loc())

        assertNull(node.findOrNull("CALL"))
    }

    @Test
    fun `find returns the descendant`() {
        val inner = leaf("ID", "a")
        val parent = SyntaxNode(name = "variable", children = listOf(inner), location = loc())

        assertSame(inner, parent.find("ID"))
    }

    @Test
    fun `find throws when the name is absent`() {
        val node = SyntaxNode(name = "expression", children = emptyList(), location = loc())
        val error = assertFailsWith<IllegalStateException> { node.find("ID") }

        assertTrue(error.message!!.contains("No descendant named 'ID' in 'expression'"))
    }

    @Test
    fun `value reads a captured token`() {
        assertEquals("pepe", leaf("ID", "pepe").value())
    }

    @Test
    fun `value throws when the node has no token`() {
        val node = SyntaxNode(name = "expression", location = loc())
        val error = assertFailsWith<IllegalStateException> { node.value() }

        assertTrue(error.message!!.contains("Node 'expression' has no token value"))
    }

    @Test
    fun `value throws when the token value is empty`() {
        val node =
            SyntaxNode(
                name = "LET",
                token = token("LET", value = null),
                location = loc(),
            )
        val error = assertFailsWith<IllegalStateException> { node.value() }

        assertTrue(error.message!!.contains("Node 'LET' has no token value"))
    }

    private fun token(
        type: String,
        value: String? = null,
    ) = Token(
        type = type,
        value = if (value == null) Optional.empty() else Optional.of(value),
        location = loc(1, 1, 1, 2),
    )

    private fun leaf(
        name: String,
        value: String? = "x",
    ) = SyntaxNode(name = name, token = token(name, value), location = loc())
}
