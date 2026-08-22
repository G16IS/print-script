package lexer.tokenization

import java.util.Optional
import printscript.reader.CharPosition
import printscript.reader.CodeReader

class MockReader(
    val statement: String,
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
