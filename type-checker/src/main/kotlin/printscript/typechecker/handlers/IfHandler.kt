package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeError

class IfHandler(
    private val checkStatement: (SyntaxNode, ScopeStack) -> StatementCheck,
) : NodeHandler {
    override val kind = "if"

    override fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): StatementCheck {
        val errors = mutableListOf<TypeError>()
        val id = node.childOrNull("ID")
        val name = id?.token?.value?.orElse(null)
        when {
            id == null || name == null ->
                errors += TypeError("if sin condición", node.location)
            scope.lookup(name) == null ->
                errors += TypeError("Variable '$name' no declarada", id.location)
            scope.lookup(name) != "boolean" ->
                errors += TypeError("Se esperaba boolean pero se encontró ${scope.lookup(name)}", id.location)
        }

        errors += checkBlock(node.childOrNull("block"), scope)

        val elseBlock =
            node
                .childOrNull("else-clause")
                ?.children
                ?.singleOrNull()
                ?.childOrNull("block")
        if (elseBlock != null) errors += checkBlock(elseBlock, scope)

        return StatementCheck(scope, errors)
    }

    private fun checkBlock(
        block: SyntaxNode?,
        parent: ScopeStack,
    ): List<TypeError> {
        if (block == null) return emptyList()
        val statements = block.childOrNull("statements")?.children ?: emptyList()
        var inner = parent.push()
        val errors = mutableListOf<TypeError>()
        for (stmt in statements) {
            val checked = checkStatement(stmt, inner)
            errors += checked.errors
            inner = checked.scope
        }
        return errors
    }
}
