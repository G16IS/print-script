package printscript.typechecker.expression

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.util.Result
import printscript.util.err

class GroupHandler(
    private val resolver: ExpressionTypeResolver,
) : ExpressionKindHandler {
    override val kind: String = "group"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val childName = config.nodes[node.name]?.expression

        val child =
            childName?.let { node.childOrNull(it) }
                ?: return err(
                    TypeError("El grupo no tiene la expresión agrupada", node.location),
                )

        return resolver.resolve(child, scope, config)
    }
}
