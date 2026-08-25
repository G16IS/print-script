package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeError
import printscript.util.Result

class PrimaryHandler(
    private val resolver: ExpressionTypeResolver,
) : ExpressionKindHandler {
    override val kind: String = "primary"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val child = node.children.singleOrNull()

        return if (child == null) {
            Result.Err(TypeError("Primario inválido", node.location))
        } else {
            resolver.resolve(child, scope, config)
        }
    }
}
