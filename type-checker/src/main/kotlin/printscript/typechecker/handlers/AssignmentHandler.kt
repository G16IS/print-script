package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeCompat
import printscript.typechecker.TypeError
import printscript.util.fold

class AssignmentHandler(
    private val resolver: ExpressionTypeResolver,
) : NodeHandler {
    override val kind = "assignment"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): StatementCheck {
        val nodeConfig = config.nodes[node.name]
        val id = nodeConfig?.id?.let { node.childOrNull(it) }
        val expressionName = nodeConfig?.expression
        val expression = expressionName?.let { node.childOrNull(it) }
        val name = id?.token?.value?.orElse(null)

        if (id == null || name == null || expression == null) {
            return StatementCheck(scope, listOf(TypeError("Asignación incompleta", node.location)))
        }

        val errors = mutableListOf<TypeError>()
        val symbol = scope.lookupSymbol(name)
        when {
            symbol == null ->
                errors += TypeError("Variable '$name' no declarada", id.location)
            !symbol.mutable ->
                errors += TypeError("No se puede asignar a la constante '$name'", id.location)
            else ->
                resolver.resolve(expression, scope, config).fold(
                    onOk = { resolved ->
                        if (!TypeCompat.compatible(symbol.type, resolved)) {
                            errors +=
                                TypeError(
                                    "Se esperaba ${symbol.type} pero se encontró $resolved",
                                    expression.location,
                                )
                        }
                    },
                    onErr = { errors += it },
                )
        }

        return StatementCheck(scope, errors)
    }
}
