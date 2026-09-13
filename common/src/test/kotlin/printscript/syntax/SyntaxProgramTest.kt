package printscript.syntax

import kotlin.test.Test
import kotlin.test.assertEquals
import printscript.support.loc

class SyntaxProgramTest {
    @Test
    fun `empty has no statements and an empty location`() {
        val program = SyntaxProgram.empty()

        assertEquals(emptyList(), program.statements)
        assertEquals(Location.empty(), program.location)
    }

    @Test
    fun `withStatement on empty uses the statement span`() {
        val stmt =
            SyntaxNode(
                name = "variable",
                location = loc(startLine = 1, startCol = 1, endLine = 1, endCol = 10),
            )

        val program = SyntaxProgram.empty().withStatement(stmt)

        assertEquals(listOf(stmt), program.statements)
        assertEquals(stmt.location.start, program.location.start)
        assertEquals(stmt.location.end, program.location.end)
    }

    @Test
    fun `withStatement on a non-empty program keeps the original start`() {
        val first =
            SyntaxNode(
                name = "variable",
                location = loc(startLine = 1, startCol = 1, endLine = 1, endCol = 10),
            )
        val second =
            SyntaxNode(
                name = "call",
                location = loc(startLine = 2, startCol = 1, endLine = 2, endCol = 12),
            )

        val program = SyntaxProgram.empty().withStatement(first).withStatement(second)

        assertEquals(listOf(first, second), program.statements)
        assertEquals(first.location.start, program.location.start)
        assertEquals(second.location.end, program.location.end)
    }
}
