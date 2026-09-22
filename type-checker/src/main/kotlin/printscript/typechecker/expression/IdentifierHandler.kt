package printscript.typechecker.expression

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack
import printscript.util.Result
import printscript.util.err
import printscript.util.ok

class IdentifierHandler : ExpressionKindHandler {
    override val kind: String = "identifier"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val name =
            node.token?.value?.orElse(null)
                ?: return err(
                    TypeError(
                        "El identificador no tiene nombre",
                        node.location,
                    ),
                )

        val type =
            scope.lookup(name)
                ?: return err(
                    TypeError("Variable '$name' no declarada", node.location),
                )

        return ok(type)
    }
}
