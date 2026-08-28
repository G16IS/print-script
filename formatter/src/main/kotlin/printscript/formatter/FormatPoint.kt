package printscript.formatter

enum class PointKind {
    BEFORE_TOKEN,
    AFTER_TOKEN,
}

data class FormatPoint(
    val kind: PointKind,
    val tokenType: String? = null,
    val tokenValue: String? = null,
    val parentNodeName: String? = null,
    val previousTokenType: String? = null,
)
