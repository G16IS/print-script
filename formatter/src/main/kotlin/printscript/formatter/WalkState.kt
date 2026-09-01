package printscript.formatter

import printscript.ast.Location
import printscript.error.FormatError
import printscript.error.WhitespaceMismatch

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
    val cursor: Int = 0,
    val indentLevel: Int = 0,
)

internal fun WalkState.withTrailingAfter(registry: RuleRegistry): WalkState {
    val last = last ?: return this
    val trailing =
        registry
            .whitespaceFor(
                FormatPoint(
                    kind = PointKind.AFTER_TOKEN,
                    tokenType = last.tokenType,
                    tokenValue = last.tokenValue,
                    parentNodeName = last.parentNodeName,
                ),
            ).render(indentLevel)
    val source = source
    val actual = source?.let { if (cursor <= it.length) it.substring(cursor) else "" }

    return when {
        source == null -> copy(output = output + trailing)
        actual == trailing -> this
        else ->
            copy(
                errors =
                    errors +
                        WhitespaceMismatch(
                            expected = trailing,
                            actual = actual.orEmpty(),
                            location = SourceGaps.locationAt(source, cursor),
                        ),
            )
    }
}
