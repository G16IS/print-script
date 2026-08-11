package printscript.util

import printscript.ast.Location
import printscript.ast.Node
import printscript.reader.CharPosition
import printscript.domain.Token

object Locations {
    fun of(token: Token): Location = Location(token.start, token.end)

    fun between(start: Token, end: Token): Location = Location(start.start, end.end)

    fun between(start: Location, end: Location): Location = Location(start.start, end.end)

    fun between(start: Node, end: Node): Location = Location(start.location.start, end.location.end)

    fun between(start: Token, end: Node): Location = Location(start.start, end.location.end)

    fun empty(): Location = Location(CharPosition(0, 0), CharPosition(0, 0))
}
