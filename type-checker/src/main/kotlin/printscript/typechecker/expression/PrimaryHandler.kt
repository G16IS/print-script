package printscript.typechecker.expression

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.util.Result
import printscript.util.err

class PrimaryHandler(
    private val resolver: ExpressionTypeResolver,
) : ExpressionKindHandler {
    override val kind: String = "primary"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val child =
            node.children.singleOrNull()
                ?: return err(TypeError("Primario inválido", node.location))

        return resolver.resolve(child, scope, config)
    }
}
