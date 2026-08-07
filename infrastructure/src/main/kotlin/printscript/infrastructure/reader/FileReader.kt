package printscript.infrastructure.reader

import printscript.common.Position
import printscript.common.reader.Reader
import java.io.File
import java.util.Optional

class FileReader(path: String): Reader {
    private val realReader = File(path).bufferedReader()
    private var currentPosition = Position(1, 1)
    private var lookahead = realReader.read()

    override fun read(): Optional<Char> {
        if (lookahead == -1) return Optional.empty()

        val char = lookahead.toChar()
        currentPosition = if (char == '\n'){
            Position(currentPosition.line + 1, 1)
        }else{
            Position(currentPosition().line, currentPosition.col + 1)
        }


        lookahead = realReader.read()
        return Optional.of(char)
    }

    override fun peek(): Optional<Char> {
        if (lookahead == -1) return Optional.empty()

        return Optional.of(lookahead.toChar())
    }

    override fun currentPosition(): Position {
        return currentPosition
    }

}
