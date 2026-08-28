package printscript.formatter

import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

internal object StatementLayouts {
    fun emit(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError>? =
        when (node.name) {
            "variable" -> emitVariable(node, state, failFast, walk)
            "call" -> emitCall(node, state, failFast, walk)
            "group" -> emitGroup(node, state, failFast, walk)
            "expression-stmt" -> emitExpressionStmt(node, state, failFast, walk)
            else -> null
        }

    private fun emitVariable(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> {
        val id = node.childOrNull("ID")
        val type = node.childOrNull("TYPE")
        val expression = node.childOrNull("expression")

        if (id == null || type == null || expression == null) {
            return Result.Err(UnrecognizedNode(node.name, node.location))
        }

        return walk
            .emitSynthetic("LET", "let", node.name, state)
            .flatMap { walk.emit(id, node.name, it, failFast) }
            .flatMap { walk.emitSynthetic("COLON", ":", node.name, it) }
            .flatMap { walk.emit(type, node.name, it, failFast) }
            .flatMap { walk.emitSynthetic("ASSIGN", "=", node.name, it) }
            .flatMap { walk.emit(expression, node.name, it, failFast) }
            .flatMap { walk.emitSynthetic("SEMICOLON", ";", node.name, it) }
    }

    private fun emitCall(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> {
        val callee = node.childOrNull("CALL")
        val args = node.children.filter { it.name != "CALL" }

        if (callee == null) {
            return Result.Err(UnrecognizedNode(node.name, node.location))
        }

        val afterOpen =
            walk
                .emit(callee, node.name, state, failFast)
                .flatMap { walk.emitSynthetic("LEFT_PAREN", "(", node.name, it) }

        val afterArgs =
            args.fold(afterOpen) { acc, arg ->
                acc.flatMap { current ->
                    walk.emit(arg, node.name, current, failFast)
                }
            }

        return afterArgs.flatMap { walk.emitSynthetic("RIGHT_PAREN", ")", node.name, it) }
    }

    private fun emitGroup(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> =
        walk
            .emitSynthetic("LEFT_PAREN", "(", node.name, state)
            .flatMap { walk.emitChildren(node, it, failFast) }
            .flatMap { walk.emitSynthetic("RIGHT_PAREN", ")", node.name, it) }

    private fun emitExpressionStmt(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> =
        walk
            .emitChildren(node, state, failFast)
            .flatMap { walk.emitSynthetic("SEMICOLON", ";", node.name, it) }
}
