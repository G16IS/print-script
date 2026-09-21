package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack
import printscript.util.Result

interface ExpressionKindHandler {
    val kind: String

    fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError>
}
