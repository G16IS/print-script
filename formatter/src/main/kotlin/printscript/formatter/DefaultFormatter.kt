package printscript.formatter

import printscript.domain.Token
import printscript.error.FormatError
import printscript.error.MissingLexeme
import printscript.error.UnrecognizedFormatNode
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Report
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.fold
import printscript.util.map

internal class DefaultFormatter(
    private val registry: RuleRegistry,
    private val grammarWalker: GrammarWalker,
) : Formatter {
    private val nodeWalk = LayoutWalk()

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
                    emitNode(statement, parentName = null, state, failFast)
                }
            }.map { state -> state.withTrailingAfter(registry) }
    }

    private fun emitNode(
        node: SyntaxNode,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> {
        val token = node.token

        if (token != null) {
            return emitToken(token, parentName, state, failFast)
        }

        return grammarWalker.emit(node, state, failFast, nodeWalk)
            ?: emitChildren(node, state, failFast)
    }

    private fun emitToken(
        token: Token,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> {
        val lexeme = token.value.orElse(null)

        return if (lexeme == null) {
            failWalk(MissingLexeme(token.type, token.location), state, failFast)
        } else {
            emitLexemePiece(registry, token.type, lexeme, parentName, token.location, state, failFast)
        }
    }

    private fun emitChildren(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError> {
        if (node.children.isEmpty()) {
            return failWalk(
                UnrecognizedFormatNode(node.name, node.location),
                state,
                failFast,
            )
        }

        val start: Result<WalkState, FormatError> = Result.Ok(state)

        return node.children
            .fold(start) { acc, child ->
                acc.flatMap { current ->
                    emitNode(child, node.name, current, failFast)
                }
            }
    }

    private inner class LayoutWalk : NodeWalk {
        override fun emit(
            node: SyntaxNode,
            parentName: String?,
            state: WalkState,
            failFast: Boolean,
        ): Result<WalkState, FormatError> = emitNode(node, parentName, state, failFast)

        override fun emitChildren(
            node: SyntaxNode,
            state: WalkState,
            failFast: Boolean,
        ): Result<WalkState, FormatError> = this@DefaultFormatter.emitChildren(node, state, failFast)

        override fun emitSynthetic(
            tokenType: String,
            lexeme: String,
            parentName: String?,
            state: WalkState,
            failFast: Boolean,
        ): Result<WalkState, FormatError> =
            emitLexemePiece(registry, tokenType, lexeme, parentName, location = null, state, failFast)
    }
}
