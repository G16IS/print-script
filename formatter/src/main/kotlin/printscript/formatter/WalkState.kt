package printscript.formatter

import printscript.ast.Location

internal data class Emitted(
    val tokenType: String,
    val tokenValue: String,
    val location: Location,
    val parentNodeName: String?,
)

internal data class WalkState(
    val output: String = "",
    val errors: List<FormatError> = emptyList(),
    val last: Emitted? = null,
    val source: String? = null,
)

internal fun WalkState.withTrailingAfter(registry: RuleRegistry): WalkState {
    val last = last

    if (last == null || source != null) {
        return this
    }

    val trailing =
        registry.whitespaceFor(
            FormatPoint(
                kind = PointKind.AFTER_TOKEN,
                tokenType = last.tokenType,
                tokenValue = last.tokenValue,
                parentNodeName = last.parentNodeName,
            ),
        )

    return copy(output = output + trailing)
}
