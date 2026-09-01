package printscript.formatter

import printscript.error.FormatError
import printscript.syntax.SyntaxNode
import printscript.util.Result

internal interface NodeWalk {
    fun emit(
        node: SyntaxNode,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError>

    fun emitChildren(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError>

    fun emitSynthetic(
        tokenType: String,
        lexeme: String,
        parentName: String?,
        state: WalkState,
        failFast: Boolean,
    ): Result<WalkState, FormatError>
}
