package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeError
import printscript.util.Result

class IdentifierHandler : ExpressionKindHandler {
    override val kind: String = "identifier"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val name =
            node.token?.value?.orElse(null) ?: return Result.Err(
                TypeError(
                    "El identificador no tiene nombre",
                    node.location,
                ),
            )

        val type = scope.lookup(name)

        return if (type == null) {
            Result.Err(TypeError("Variable '$name' no declarada", node.location))
        } else {
            Result.Ok(type)
        }
    }
}
