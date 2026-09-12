package printscript.walk

import kotlin.test.Test
import kotlin.test.assertEquals
import printscript.syntax.Location
import printscript.syntax.SyntaxNode

class SyntaxTreeWalkerTest {
    private val loc = Location.empty()

    @Test
    fun `walks single node with no children`() {
        val leaf = SyntaxNode(name = "identifier", children = emptyList(), location = loc)
        val visited = SyntaxTreeWalker.walk(leaf)

        assertEquals(listOf("identifier"), visited.map { it.name })
    }

    @Test
    fun `walks tree in pre-order DFS`() {
        // Tree:
        //      variable
        //     /    |   \
        //   ID   TYPE  expression
        //                /    \
        //              term   number
        val idNode = SyntaxNode(name = "ID", location = loc)
        val typeNode = SyntaxNode(name = "TYPE", location = loc)
        val termNode = SyntaxNode(name = "term", location = loc)
        val numNode = SyntaxNode(name = "number", location = loc)
        val exprNode = SyntaxNode(name = "expression", children = listOf(termNode, numNode), location = loc)
        val root = SyntaxNode(name = "variable", children = listOf(idNode, typeNode, exprNode), location = loc)

        val visited = SyntaxTreeWalker.walk(root)

        val expectedNames = listOf("variable", "ID", "TYPE", "expression", "term", "number")
        assertEquals(expectedNames, visited.map { it.name })
    }

    @Test
    fun `walks deep nested single-child chain`() {
        val leaf = SyntaxNode(name = "leaf", location = loc)
        val mid = SyntaxNode(name = "mid", children = listOf(leaf), location = loc)
        val root = SyntaxNode(name = "root", children = listOf(mid), location = loc)

        val visited = SyntaxTreeWalker.walk(root)

        assertEquals(listOf("root", "mid", "leaf"), visited.map { it.name })
    }
}
