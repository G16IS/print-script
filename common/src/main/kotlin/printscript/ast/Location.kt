package printscript.ast

import printscript.reader.CharPosition

data class Location(
    val start: CharPosition,
    val end: CharPosition,
) {
    companion object {
        fun empty(): Location =
            Location(
                CharPosition(0, 0),
                CharPosition(0, 0),
            )
    }
}
