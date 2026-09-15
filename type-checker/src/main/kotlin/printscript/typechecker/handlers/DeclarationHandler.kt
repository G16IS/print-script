package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeError
import printscript.util.fold

class DeclarationHandler(
    private val resolver: ExpressionTypeResolver,
) : NodeHandler {
    override val kind: String = "declaration"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): StatementCheck {
        val parts = parts(node, config)

        return if (parts == null) {
            StatementCheck(scope, listOf(TypeError("Declaración incompleta", node.location)))
        } else {
            checkParts(parts, scope, config)
        }
    }

    private fun checkParts(
        parts: DeclarationParts,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): StatementCheck {
        val errors = mutableListOf<TypeError>()
        if (parts.declared !in config.types) {
            errors += TypeError("Tipo desconocido '${parts.declared}'", parts.typeNode.location)
        }

        parts.expression?.let { expression ->
            resolver.resolve(expression, scope, config).fold(
                onOk = { resolved ->
                    if (parts.declared in config.types && parts.declared != resolved) {
                        errors +=
                            TypeError(
                                "Se esperaba ${parts.declared} pero se encontró $resolved",
                                expression.location,
                            )
                    }
                },
                onErr = { errors += it },
            )
        }

        return StatementCheck(declare(parts, scope, config, errors), errors)
    }

    private fun declare(
        parts: DeclarationParts,
        scope: ScopeStack,
        config: TypeSystemConfig,
        errors: MutableList<TypeError>,
    ): ScopeStack {
        if (parts.declared !in config.types) return scope
        val declared = scope.declare(parts.name, parts.declared)

        return if (declared == null) {
            errors += TypeError("La variable '${parts.name}' ya fue declarada", parts.id.location)
            scope
        } else {
            declared
        }
    }

    private fun parts(
        node: SyntaxNode,
        config: TypeSystemConfig,
    ): DeclarationParts? {
        val nodeConfig = config.nodes[node.name]
        val id = nodeConfig?.id?.let { node.childOrNull(it) }
        val typeNode = nodeConfig?.declaredType?.let { node.childOrNull(it) }
        val expressionName = nodeConfig?.expression
        return if (expressionName == null || id == null || typeNode == null) {
            null
        } else {
            namedParts(id, typeNode, node.findOrNull(expressionName))
        }
    }

    private fun namedParts(
        id: SyntaxNode,
        typeNode: SyntaxNode,
        expression: SyntaxNode?,
    ): DeclarationParts? {
        val name = id.token?.value?.orElse(null)
        val declared = typeNode.token?.value?.orElse(null)

        return if (name == null || declared == null) {
            null
        } else {
            DeclarationParts(id, name, typeNode, declared, expression)
        }
    }

    private data class DeclarationParts(
        val id: SyntaxNode,
        val name: String,
        val typeNode: SyntaxNode,
        val declared: String,
        val expression: SyntaxNode?,
    )
}
