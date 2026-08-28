package printscript.formatter

import printscript.ast.Location
import printscript.domain.Token
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Report
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.fold
import printscript.util.map

class DefaultFormatter(
    private val registry: RuleRegistry,
) : Formatter {
    override fun format(program: SyntaxProgram): Result<String, FormatError> =
        walk(program.statements, WalkState(), failFast = true)
            .map { it.output }

    override fun check(
        program: SyntaxProgram,
        source: String,
    ): Report<Unit, FormatError> =
        walk(program.statements, WalkState(source = source), failFast = false)
            .fold(
                onOk = { Report(value = Unit, errors = it.errors) },
                onErr = { Report(value = Unit, errors = listOf(it)) },
            )

    private fun walk(
        statements: List<SyntaxNode>,
        initial: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> {
        val start: Result<WalkState, FormatError> = Result.Ok(initial)

        return statements
            .fold(start) { acc, statement ->
                acc.flatMap { state ->
                    emit(statement, parentName = null, state, failFast)
                }
            }
    }

    private fun emit(
        node: SyntaxNode,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> {
        val token = node.token

        return if (token != null) {
            emitToken(token, parentName, state, failFast)
        } else {
            emitChildren(node, state, failFast)
        }
    }

    private fun emitToken(
        token: Token,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> {
        val lexeme = token.value.orElse(null)

        return if (lexeme == null) {
            fail(MissingLexeme(token.type, token.location), state, failFast)
        } else {
            emitLexeme(token, lexeme, parentName, state, failFast)
        }
    }

    private fun emitChildren(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> {
        if (node.children.isEmpty()) {
            return fail(
                UnrecognizedNode(node.name, node.location),
                state,
                failFast,
            )
        }

        val start: Result<WalkState, FormatError> = Result.Ok(state)

        return node.children
            .fold(start) { acc, child ->
                acc.flatMap { current ->
                    emit(child, node.name, current, failFast)
                }
            }
    }

    private fun emitLexeme(
        token: Token,
        lexeme: String,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> {
        val expected = expectedWhitespace(state.last, token, lexeme, parentName)
        val mismatch = whitespaceMismatch(state, token, expected)

        if (mismatch != null && failFast) {
            return Result.Err(mismatch)
        }

        val output =
            if (state.source == null) {
                state.output + expected + lexeme
            } else {
                state.output
            }

        val errors =
            if (mismatch == null) {
                state.errors
            } else {
                state.errors + mismatch
            }

        val next =
            state.copy(
                output = output,
                errors = errors,
                last =
                    Emitted(
                        tokenType = token.type,
                        tokenValue = lexeme,
                        location = token.location,
                        parentNodeName = parentName,
                    ),
            )

        return Result.Ok(next)
    }

    private fun whitespaceMismatch(
        state: WalkState,
        token: Token,
        expected: String,
    ): WhitespaceMismatch? {
        val previous = state.last
        val source = state.source

        if (previous == null || source == null) {
            return null
        }

        val actual =
            SourceGaps.between(
                source,
                previous.location.end,
                token.location.start,
            )

        return if (actual == expected) {
            null
        } else {
            WhitespaceMismatch(expected, actual, token.location)
        }
    }

    private fun expectedWhitespace(
        previous: Emitted?,
        token: Token,
        lexeme: String,
        parentName: String?,
    ): String {
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
            } ?: ""

        val beforeCurrent =
            registry.whitespaceFor(
                FormatPoint(
                    kind = PointKind.BEFORE_TOKEN,
                    tokenType = token.type,
                    tokenValue = lexeme,
                    parentNodeName = parentName,
                ),
            )

        return afterPrevious + beforeCurrent
    }

    private fun fail(
        error: FormatError,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> =
        if (failFast) {
            Result.Err(error)
        } else {
            Result.Ok(state.copy(errors = state.errors + error))
        }

    private data class Emitted(
        val tokenType: String,
        val tokenValue: String,
        val location: Location,
        val parentNodeName: String?,
    )

    private data class WalkState(
        val output: String = "",
        val errors: List<FormatError> = emptyList(),
        val last: Emitted? = null,
        val source: String? = null,
    )
}
