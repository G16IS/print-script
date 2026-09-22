package printscript.typechecker.expression

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack
import printscript.util.Result
import printscript.util.err
import printscript.util.ok

class LiteralHandler : ExpressionKindHandler {
    override val kind: String = "literal"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val token =
            node.token
                ?: return err(
                    TypeError(
                        "El literal no tiene token",
                        node.location,
                    ),
                )

        val type =
            config.literals[token.type] ?: return err(
                TypeError(
                    "Literal de token '${token.type}' no tiene tipo en la configuración",
                    node.location,
                ),
            )

        return ok(type)
    }
}
