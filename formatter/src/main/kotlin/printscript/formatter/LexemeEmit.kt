package printscript.formatter

import printscript.ast.Location
import printscript.error.FormatError
import printscript.error.MissingLexeme
import printscript.error.WhitespaceMismatch
import printscript.util.Result

internal fun emitLexemePiece(
    registry: RuleRegistry,
    tokenType: String,
    lexeme: String,
    parentName: String?,
    location: Location?,
    state: WalkState,
    failFast: Boolean,
): Result<WalkState, FormatError> {
    val expected = expectedWhitespace(registry, state, tokenType, lexeme, parentName)
    val source = state.source

    if (source == null) {
        return Result.Ok(
            state.copy(
                output = state.output + expected + lexeme,
                last =
                    Emitted(
                        tokenType = tokenType,
                        tokenValue = lexeme,
                        location = location ?: Location.empty(),
                        parentNodeName = parentName,
                    ),
            ),
        )
    }

    val found = lexemeStart(source, state.cursor, lexeme)
    val mismatch =
        if (found == null) {
            null
        } else {
            gapMismatch(source, state.cursor, found, expected)
        }

    return when {
        found == null ->
            failWalk(
                MissingLexeme(tokenType, location ?: SourceGaps.locationAt(source, state.cursor)),
                state,
                failFast,
            )
        mismatch != null && failFast -> Result.Err(mismatch)
        else ->
            Result.Ok(
                state.copy(
                    errors = if (mismatch == null) state.errors else state.errors + mismatch,
                    last =
                        Emitted(
                            tokenType = tokenType,
                            tokenValue = lexeme,
                            location = SourceGaps.span(source, found, found + lexeme.length),
                            parentNodeName = parentName,
                        ),
                    cursor = found + lexeme.length,
                ),
            )
    }
}

internal fun expectedWhitespace(
    registry: RuleRegistry,
    state: WalkState,
    tokenType: String,
    lexeme: String,
    parentName: String?,
): String = expectedGap(registry, state.last, tokenType, lexeme, parentName).render(state.indentLevel)

private fun expectedGap(
    registry: RuleRegistry,
    previous: Emitted?,
    tokenType: String,
    lexeme: String,
    parentName: String?,
): Gap {
    val afterPrevious =
        previous?.let { emitted ->
            registry.whitespaceFor(
                FormatPoint(
                    kind = PointKind.AFTER_TOKEN,
                    tokenType = emitted.tokenType,
                    tokenValue = emitted.tokenValue,
                    parentNodeName = emitted.parentNodeName,
                ),
            )
        } ?: Gap.EMPTY

    val beforeCurrent =
        registry.whitespaceFor(
            FormatPoint(
                kind = PointKind.BEFORE_TOKEN,
                tokenType = tokenType,
                tokenValue = lexeme,
                parentNodeName = parentName,
                previousTokenType = previous?.tokenType,
            ),
        )

    return afterPrevious + beforeCurrent
}

internal fun failWalk(
    error: FormatError,
    state: WalkState,
    failFast: Boolean,
): Result<WalkState, FormatError> =
    if (failFast) {
        Result.Err(error)
    } else {
        Result.Ok(state.copy(errors = state.errors + error))
    }

private fun lexemeStart(
    source: String,
    cursor: Int,
    lexeme: String,
): Int? {
    val at = source.indexOf(lexeme, cursor)
    return if (at < 0) null else at
}

private fun gapMismatch(
    source: String,
    cursor: Int,
    lexemeStart: Int,
    expected: String,
): WhitespaceMismatch? {
    val actual = source.substring(cursor, lexemeStart.coerceAtLeast(cursor))

    return if (actual == expected) {
        null
    } else {
        WhitespaceMismatch(expected, actual, SourceGaps.locationAt(source, lexemeStart))
    }
}
