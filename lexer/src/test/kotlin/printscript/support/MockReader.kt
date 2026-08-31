package printscript.support

import java.util.Optional
import printscript.reader.CharPosition
import printscript.reader.CodeReader

/**
 * In-memory [CodeReader] for lexer tests.
 *
 * Positions are `(line = 0, col = index)` after each `read()`. That is not how
 * [printscript.infrastructure.reader.FileCodeReader] counts (1-based lines).
 * Do not compare locations between the two.
 */
class MockReader(
    private val statement: String,
) : CodeReader {
    var index: Int = 0

    override fun read(): Optional<Char> {
        if (index >= statement.length) return Optional.empty()
        val result = Optional.of(statement[index])
        index++
        return result
    }

    override fun peek(): Optional<Char> {
        if (index >= statement.length) return Optional.empty()
        return Optional.of(statement[index])
    }

    override fun currentPosition(): CharPosition = CharPosition(0, index)
}
