package printscript.formatter

import printscript.ast.Location
import printscript.domain.Token
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Report
import printscript.util.Result

class DefaultFormatter(
    private val registry: RuleRegistry,
) : Formatter {
    override fun format(program: SyntaxProgram): Result<String, FormatError> {
        val state = WalkState(registry)

        for (statement in program.statements) {
            when (val result = emit(
                statement, parentName = null, state, failFast = true
            )) {
                is Result.Err -> return result
                is Result.Ok -> Unit
            }
        }

        return Result.Ok(state.buffer.toString())
    }

    override fun check(
        program: SyntaxProgram,
        source: String,
    ): Report<Unit, FormatError> {
        val state = WalkState(registry, source = source)

        for (statement in program.statements) {
            emit(statement, parentName = null, state, failFast = false)
        }

        return Report(value = Unit, errors = state.errors)
    }

    private fun emit(
        node: SyntaxNode,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<Unit, FormatError> {
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
    ): Result<Unit, FormatError> {
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
    ): Result<Unit, FormatError> =
        if (node.children.isEmpty()) {
            fail(UnrecognizedNode(node.name, node.location), state, failFast)
        } else {
            emitEachChild(node, state, failFast)
        }

    private fun emitEachChild(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
    ): Result<Unit, FormatError> {
        for (child in node.children) {
            when (val result = emit(child, node.name, state, failFast)) {
                is Result.Err -> return result
                is Result.Ok -> Unit
            }
        }
        return Result.Ok(Unit)
    }

    private fun emitLexeme(
        token: Token,
        lexeme: String,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<Unit, FormatError> {
        val expected = expectedWhitespace(state, token, lexeme, parentName)
        if (state.source != null && state.last != null) {
            val actual =
                SourceGaps.between(
                    state.source,
                    state.last!!.location.end,
                    token.location.start,
                )
            if (actual != expected) {
                val error = WhitespaceMismatch(expected, actual, token.location)
                if (failFast) return Result.Err(error)
                state.errors += error
            }
        }
        if (state.source == null) {
            state.buffer.append(expected)
            state.buffer.append(lexeme)
        }
        state.last =
            Emitted(
                tokenType = token.type,
                tokenValue = lexeme,
                location = token.location,
                parentNodeName = parentName,
            )
        return Result.Ok(Unit)
    }

    private fun expectedWhitespace(
        state: WalkState,
        token: Token,
        lexeme: String,
        parentName: String?,
    ): String {
        val afterPrevious =
            state.last?.let { previous ->
                state.registry.whitespaceFor(
                    FormatPoint(
                        kind = PointKind.AFTER_TOKEN,
                        tokenType = previous.tokenType,
                        tokenValue = previous.tokenValue,
                        parentNodeName = previous.parentNodeName,
                    ),
                )
            } ?: ""
        val beforeCurrent =
            state.registry.whitespaceFor(
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
    ): Result<Unit, FormatError> {
        if (failFast) return Result.Err(error)
        state.errors += error
        return Result.Ok(Unit)
    }

    private data class Emitted(
        val tokenType: String,
        val tokenValue: String,
        val location: Location,
        val parentNodeName: String?,
    )

    private class WalkState(
        val registry: RuleRegistry,
        val source: String? = null,
    ) {
        val buffer = StringBuilder()
        val errors = mutableListOf<FormatError>()
        var last: Emitted? = null
    }
}
