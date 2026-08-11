package printscript.common.reader

import java.util.Optional

interface CodeReader {
    fun read(): Optional<Char>
    fun peek(): Optional<Char>
    fun currentPosition(): CharPosition
}
