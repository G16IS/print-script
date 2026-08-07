package printscript.common.reader

import printscript.common.Position
import java.util.Optional

interface Reader {
    fun read(): Optional<Char>
    fun peek(): Optional<Char>
    fun currentPosition(): Position
}
