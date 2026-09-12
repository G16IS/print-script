package printscript.formatter

import printscript.reader.CharPosition
import printscript.syntax.Location

internal object SourceGaps {
    fun position(
        source: String,
        offset: Int,
    ): CharPosition {
        val clamped = offset.coerceIn(0, source.length)
        val before = source.substring(0, clamped)
        val line = before.count { it == '\n' } + 1
        val lastNewline = before.lastIndexOf('\n')
        val col = if (lastNewline < 0) clamped + 1 else clamped - lastNewline

        return CharPosition(line, col)
    }

    fun locationAt(
        source: String,
        offset: Int,
    ): Location {
        val start = position(source, offset)
        return Location(start, start)
    }

    fun span(
        source: String,
        startOffset: Int,
        endExclusive: Int,
    ): Location {
        val endInclusive = (endExclusive - 1).coerceAtLeast(startOffset)
        return Location(position(source, startOffset), position(source, endInclusive))
    }
}
