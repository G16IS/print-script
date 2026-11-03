package printscript.reader

import java.util.Optional

/** [CodeReader] in-memory; las posiciones coinciden con [FileCodeReader] (1-based). */
class StringCodeReader(
    source: String,
) : CodeReader {
    private val realReader = source.reader().buffered()
    private var currentPosition = CharPosition(1, 1)
    private var lookahead = realReader.read()

    override fun read(): Optional<Char> {
        if (lookahead == -1) return Optional.empty()

        val char = lookahead.toChar()
        currentPosition =
            if (char == '\n') {
                CharPosition(currentPosition.line + 1, 1)
            } else {
                CharPosition(currentPosition.line, currentPosition.col + 1)
            }

        lookahead = realReader.read()
        return Optional.of(char)
    }

    override fun peek(): Optional<Char> {
        if (lookahead == -1) return Optional.empty()
        return Optional.of(lookahead.toChar())
    }

    override fun currentPosition(): CharPosition = currentPosition
}
