package lexer

import printscript.common.reader.CharPosition
import printscript.common.reader.CodeReader
import java.util.Optional

class MockReader(val statement: String) : CodeReader {
    var index: Int = 0
    override fun read(): Optional<Char> {
        val result = Optional.of(statement[index])
        index++
        return result
    }

    override fun peek(): Optional<Char> {
        return Optional.of(statement[index])
    }

    override fun currentPosition(): CharPosition {
        return CharPosition(0, index)
    }
}
