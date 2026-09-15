package printscript.syntax

import printscript.reader.CharPosition

data class SyntaxProgram(
    val statements: List<SyntaxNode>,
    val location: Location,
) {
    fun withStatement(statement: SyntaxNode): SyntaxProgram =
        SyntaxProgram(
            statements =
                buildList(statements.size + 1) {
                    addAll(statements)
                    add(statement)
                },
            location = Location(startOf(statement), statement.location.end),
        )

    private fun startOf(statement: SyntaxNode): CharPosition =
        if (statements.isEmpty()) statement.location.start else location.start

    companion object {
        fun empty(): SyntaxProgram = SyntaxProgram(emptyList(), Location.empty())

        fun builder(): Builder = Builder()
    }

    /**
     * Accumulates statements with amortized O(1) append.
     * Call [build] once to produce the immutable [SyntaxProgram].
     */
    class Builder {
        private val statements = ArrayList<SyntaxNode>()
        private var start: CharPosition? = null

        fun add(statement: SyntaxNode): Builder {
            if (start == null) start = statement.location.start
            statements.add(statement)
            return this
        }

        fun build(): SyntaxProgram {
            if (statements.isEmpty()) return empty()
            return SyntaxProgram(
                statements = statements.toList(),
                location = Location(start!!, statements.last().location.end),
            )
        }
    }
}
