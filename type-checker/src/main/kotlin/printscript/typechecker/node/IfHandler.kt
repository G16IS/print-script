package printscript.typechecker.node

import printscript.domain.NodeConfig
import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeCompat
import printscript.util.Result

class IfHandler(
    private val resolver: ExpressionTypeResolver,
    private val checkStatement: (SyntaxNode, ScopeStack) -> Checked,
) : NodeHandler {
    override val kind = "if"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Checked {
        val nodeConfig = config.nodes[node.name]
        val error =
            checkCondition(node, nodeConfig, scope, config)
                ?: checkBranch(node.childOrNull(nodeConfig?.then.orEmpty()), nodeConfig, scope)
                ?: checkBranch(elseBlock(node, nodeConfig), nodeConfig, scope)
        return Checked(scope, error)
    }

    private fun checkCondition(
        node: SyntaxNode,
        nodeConfig: NodeConfig?,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): TypeError? {
        val condition =
            node.childOrNull(nodeConfig?.expression.orEmpty())
                ?: return TypeError("if sin condición", node.location)

        return when (val resolved = resolver.resolve(condition, scope, config)) {
            is Result.Err -> resolved.error
            is Result.Ok ->
                if (TypeCompat.compatible(BOOLEAN, resolved.value)) {
                    null
                } else {
                    TypeError(
                        "Se esperaba boolean pero se encontró ${resolved.value}",
                        condition.location,
                    )
                }
        }
    }

    private fun elseBlock(
        node: SyntaxNode,
        nodeConfig: NodeConfig?,
    ): SyntaxNode? =
        node
            .childOrNull(nodeConfig?.elseClause.orEmpty())
            ?.children
            ?.singleOrNull()
            ?.childOrNull(nodeConfig?.then.orEmpty())

    private fun checkBranch(
        block: SyntaxNode?,
        nodeConfig: NodeConfig?,
        parent: ScopeStack,
    ): TypeError? {
        if (block == null) return null
        val statements =
            block.childOrNull(nodeConfig?.block.orEmpty())?.children
                ?: emptyList()
        var inner = parent.push()
        for (statement in statements) {
            val checked = checkStatement(statement, inner)
            checked.error?.let { return it }
            inner = checked.scope
        }
        return null
    }

    private companion object {
        const val BOOLEAN = "boolean"
    }
}
