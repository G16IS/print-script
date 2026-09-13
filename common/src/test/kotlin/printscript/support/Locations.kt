package printscript.support

import printscript.ast.Location
import printscript.reader.CharPosition

fun loc(
    startLine: Int = 1,
    startCol: Int = 1,
    endLine: Int = 1,
    endCol: Int = 1,
): Location =
    Location(
        CharPosition(startLine, startCol),
        CharPosition(endLine, endCol),
    )
