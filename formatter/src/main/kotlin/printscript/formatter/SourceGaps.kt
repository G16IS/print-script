package printscript.formatter

import printscript.ast.Location
import printscript.reader.CharPosition

internal object SourceGaps {
    fun offset(
        source: String,
        position: CharPosition,
    ): Int {
        val lines = source.split('\n')
        val lineIndex = if (position.line <= 0) 0 else position.line - 1
        val columnIndex = if (position.col <= 0) 0 else position.col - 1
        val before = lines.take(lineIndex).sumOf { it.length + 1 }

        return (before + columnIndex).coerceIn(0, source.length)
    }

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
