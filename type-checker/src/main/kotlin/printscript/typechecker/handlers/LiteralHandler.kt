package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeError
import printscript.util.Result

class LiteralHandler : ExpressionKindHandler {
    override val kind: String = "literal"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val token =
            node.token
                ?: return Result.Err(
                    TypeError("El literal no tiene token", node.location),
                )

        val type = config.literals[token.type]

        return if (type == null) {
            Result.Err(
                TypeError(
                    "Literal de token '${token.type}' no tiene tipo en la configuración",
                    node.location,
                ),
            )
        } else {
            Result.Ok(type)
        }
    }
}
