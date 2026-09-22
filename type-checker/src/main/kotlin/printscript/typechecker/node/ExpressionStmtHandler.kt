package printscript.typechecker.node

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.util.fold

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
                ?: return Checked(
                    scope,
                    TypeError(
                        "Statement de expresión inválido",
                        node.location,
                    ),
                )

        return resolver.resolve(expression, scope, config).fold(
            onErr = { resolved -> Checked(scope, resolved) },
            onOk = { Checked(scope) },
        )
    }
}
