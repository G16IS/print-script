package printscript.syntax

import printscript.ast.Location
import printscript.reader.CharPosition

data class SyntaxProgram(
    val statements: List<SyntaxNode>,
    val location: Location
) {
    fun withStatement(statement: SyntaxNode): SyntaxProgram =
        SyntaxProgram(
            statements = statements + statement,
            location = Location(startOf(statement), statement.location.end)
        )

    private fun startOf(statement: SyntaxNode): CharPosition =
        if (statements.isEmpty()) statement.location.start else location.start

    companion object {
        fun empty(): SyntaxProgram =
            SyntaxProgram(emptyList(), Location.empty())
    }
}
