package printscript.parser.util

import printscript.common.ast.Location
import printscript.common.ast.Node
import printscript.common.reader.CharPosition
import printscript.common.domain.Token

object Locations {
    fun of(token: Token): Location = Location(token.start, token.end)

    fun between(start: Token, end: Token): Location = Location(start.start, end.end)

    fun between(start: Location, end: Location): Location = Location(start.start, end.end)

    fun between(start: Node, end: Node): Location = Location(start.location.start, end.location.end)

    fun between(start: Token, end: Node): Location = Location(start.start, end.location.end)

    fun empty(): Location = Location(CharPosition(0, 0), CharPosition(0, 0))
}
