package printscript.common.reader

import printscript.common.reader.CharPosition
import java.util.Optional

interface CodeReader {
    fun read(): Optional<Char>
    fun peek(): Optional<Char>
    fun currentPosition(): CharPosition
}
