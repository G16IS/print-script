package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeError
import printscript.util.fold

class ExpressionStmtHandler(
    private val resolver: ExpressionTypeResolver,
) : NodeHandler {
    override val kind: String = "expression"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): StatementCheck {
        val childName = config.nodes[node.name]?.expression
        val expression = childName?.let { node.childOrNull(it) }

        if (expression == null) {
            return StatementCheck(
                scope,
                listOf(TypeError("Statement de expresión inválido", node.location)),
            )
        }

        val errors =
            resolver.resolve(expression, scope, config).fold(
                onOk = { emptyList() },
                onErr = { listOf(it) },
            )

        return StatementCheck(scope, errors)
    }
}
