package printscript.formatter

/** AFTER + BEFORE between two tokens. [render] adds indent only after newlines. */
internal data class Gap(
    val newlines: Int = 0,
    val spaces: Int = 0,
) {
    operator fun plus(other: Gap): Gap =
        Gap(
            newlines = newlines + other.newlines,
            spaces = (spaces + other.spaces).coerceIn(0, 1),
        )

    fun render(indentLevel: Int): String {
        if (newlines <= 0) {
            return " ".repeat(spaces.coerceIn(0, 1))
        }

        val indent = indentLevel.coerceAtLeast(0) * INDENT_WIDTH
        return "\n".repeat(newlines) + " ".repeat(indent)
    }

    companion object {
        val EMPTY = Gap()
        const val INDENT_WIDTH = 4
    }
}
