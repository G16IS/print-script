package printscript.formatter

import printscript.reader.CharPosition

internal object SourceGaps {
    fun between(
        source: String,
        previousEndInclusive: CharPosition,
        currentStartInclusive: CharPosition,
    ): String {
        val from = offset(source, previousEndInclusive) + 1
        val to = offset(source, currentStartInclusive)

        if (from !in 0..<to || to > source.length) {
            return ""
        }

        return source.substring(from, to)
    }

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
}
