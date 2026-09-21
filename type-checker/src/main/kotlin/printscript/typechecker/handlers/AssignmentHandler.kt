package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeCompat
import printscript.util.Result

class AssignmentHandler(
    private val resolver: ExpressionTypeResolver,
) : NodeHandler {
    override val kind = "assignment"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Checked {
        val nodeConfig = config.nodes[node.name]
        val id = nodeConfig?.id?.let { node.childOrNull(it) }
        val expression = nodeConfig?.expression?.let { node.childOrNull(it) }
        val name = id?.token?.value?.orElse(null)
        if (id == null || name == null || expression == null) {
            return Checked(scope, TypeError("Asignación incompleta", node.location))
        }

        val symbol =
            scope.lookupSymbol(name)
                ?: return Checked(scope, TypeError("Variable '$name' no declarada", id.location))
        if (!symbol.mutable) {
            return Checked(scope, TypeError("No se puede asignar a la constante '$name'", id.location))
        }

        return when (val resolved = resolver.resolve(expression, scope, config)) {
            is Result.Err -> Checked(scope, resolved.error)
            is Result.Ok ->
                if (TypeCompat.compatible(symbol.type, resolved.value)) {
                    Checked(scope)
                } else {
                    Checked(
                        scope,
                        TypeError(
                            "Se esperaba ${symbol.type} pero se encontró ${resolved.value}",
                            expression.location,
                        ),
                    )
                }
        }
    }
}
