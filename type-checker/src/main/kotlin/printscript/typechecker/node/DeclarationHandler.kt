package printscript.typechecker.node

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeCompat
import printscript.util.Result

class DeclarationHandler(
    private val resolver: ExpressionTypeResolver,
) : NodeHandler {
    override val kind: String = "declaration"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Checked {
        val nodeConfig = config.nodes[node.name] ?: return incomplete(node, scope)
        val expressionName = nodeConfig.expression ?: return incomplete(node, scope)
        val id = nodeConfig.id?.let { node.childOrNull(it) } ?: return incomplete(node, scope)
        val typeNode = nodeConfig.declaredType?.let { node.childOrNull(it) } ?: return incomplete(node, scope)
        val name = id.token?.value?.orElse(null) ?: return incomplete(node, scope)
        val declared = typeNode.token?.value?.orElse(null) ?: return incomplete(node, scope)

        val expression = node.findOrNull(expressionName)
        val resolved = expression?.let { resolver.resolve(it, scope, config) }
        val error =
            if (declared !in config.types) {
                TypeError("Tipo desconocido '$declared'", typeNode.location)
            } else {
                initializerError(declared, expression, resolved)
            }

        if (declared !in config.types) return Checked(scope, error)

        val updated =
            scope.declare(name, declared, nodeConfig.mutable)
                ?: return Checked(
                    scope,
                    error ?: TypeError("La variable '$name' ya fue declarada", id.location),
                )
        return Checked(updated, error)
    }

    private fun incomplete(
        node: SyntaxNode,
        scope: ScopeStack,
    ) = Checked(scope, TypeError("Declaración incompleta", node.location))

    private fun initializerError(
        declared: String,
        expression: SyntaxNode?,
        resolved: Result<String, TypeError>?,
    ): TypeError? =
        when (resolved) {
            null -> null
            is Result.Err -> resolved.error
            is Result.Ok ->
                if (expression == null || TypeCompat.compatible(declared, resolved.value)) {
                    null
                } else {
                    TypeError(
                        "Se esperaba $declared pero se encontró ${resolved.value}",
                        expression.location,
                    )
                }
        }
}
