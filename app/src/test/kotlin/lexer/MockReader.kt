package lexer

import lexer.reader.Reader
import token.Position
import java.util.Optional

class MockReader(val statement: String): Reader {
    var index: Int = 0
    override fun read(): Optional<Char> {
        val result = Optional.of(statement[index])
        index++
        return result
    }

    override fun peek(): Optional<Char> {
        return Optional.of(statement[index])
    }

    override fun currentPosition(): Position {
        return Position(0, index)
    }
}
