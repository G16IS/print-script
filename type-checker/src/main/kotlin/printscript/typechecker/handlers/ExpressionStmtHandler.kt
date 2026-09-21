package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.util.Result

class ExpressionStmtHandler(
    private val resolver: ExpressionTypeResolver,
) : NodeHandler {
    override val kind: String = "expression"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Checked {
        val expression =
            config.nodes[node.name]?.expression?.let { node.childOrNull(it) }
                ?: return Checked(scope, TypeError("Statement de expresión inválido", node.location))

        return when (val resolved = resolver.resolve(expression, scope, config)) {
            is Result.Err -> Checked(scope, resolved.error)
            is Result.Ok -> Checked(scope)
        }
    }
}
