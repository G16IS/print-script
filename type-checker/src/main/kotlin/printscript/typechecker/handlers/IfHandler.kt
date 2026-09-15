package printscript.typechecker.handlers

import printscript.domain.NodeConfig
import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeCompat
import printscript.typechecker.TypeError
import printscript.util.fold

/**
 * `if (<expr boolean>) { … } (else { … })?`
 *
 * La condición se resuelve como cualquier otra expresión y tiene que dar
 * `boolean`; eso cubre tanto `if (flag)` como `if (true)` o `if (readEnv("X"))`.
 * Los nombres de los hijos vienen del [NodeConfig], no hardcodeados.
 */
class IfHandler(
    private val resolver: ExpressionTypeResolver,
    private val checkStatement: (SyntaxNode, ScopeStack) -> StatementCheck,
) : NodeHandler {
    override val kind = "if"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): StatementCheck {
        val nodeConfig = config.nodes[node.name]
        val errors = mutableListOf<TypeError>()

        errors += checkCondition(node, nodeConfig, scope, config)
        errors += checkBranch(node.childOrNull(nodeConfig?.then.orEmpty()), nodeConfig, scope, config)
        errors += checkBranch(elseBlock(node, nodeConfig), nodeConfig, scope, config)

        return StatementCheck(scope, errors)
    }

    private fun checkCondition(
        node: SyntaxNode,
        nodeConfig: NodeConfig?,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): List<TypeError> {
        val condition = node.childOrNull(nodeConfig?.expression.orEmpty())
        if (condition == null) {
            return listOf(TypeError("if sin condición", node.location))
        }

        val errors = mutableListOf<TypeError>()
        resolver.resolve(condition, scope, config).fold(
            onOk = { resolved ->
                if (!TypeCompat.compatible(BOOLEAN, resolved)) {
                    errors +=
                        TypeError(
                            "Se esperaba boolean pero se encontró $resolved",
                            condition.location,
                        )
                }
            },
            onErr = { errors += it },
        )
        return errors
    }

    /** `else-clause` es un `optional`: envuelve 0 o 1 `else-block`. */
    private fun elseBlock(
        node: SyntaxNode,
        nodeConfig: NodeConfig?,
    ): SyntaxNode? =
        node
            .childOrNull(nodeConfig?.elseClause.orEmpty())
            ?.children
            ?.singleOrNull()
            ?.childOrNull(nodeConfig?.then.orEmpty())

    /** Cada bloque abre un scope propio: lo que se declara adentro no escapa. */
    private fun checkBranch(
        block: SyntaxNode?,
        nodeConfig: NodeConfig?,
        parent: ScopeStack,
        config: TypeSystemConfig,
    ): List<TypeError> {
        if (block == null) return emptyList()
        val statements = block.childOrNull(nodeConfig?.block.orEmpty())?.children ?: emptyList()
        var inner = parent.push()
        val errors = mutableListOf<TypeError>()
        for (statement in statements) {
            val checked = checkStatement(statement, inner)
            errors += checked.errors
            inner = checked.scope
        }
        return errors
    }

    private companion object {
        const val BOOLEAN = "boolean"
    }
}
