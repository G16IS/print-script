package printscript.parser.util

import printscript.syntax.Location
import printscript.syntax.SyntaxNode

object Locations {
    fun span(
        nodes: List<SyntaxNode>,
        fallback: Location,
    ): Location {
        val first = nodes.firstOrNull() ?: return fallback
        return Location(first.location.start, nodes.last().location.end)
    }
}
