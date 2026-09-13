package printscript.typechecker

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface ExpressionTypeResolver {
    fun resolve(
        expression: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError>
}
