package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeError

data class StatementCheck(
    val scope: ScopeStack,
    val errors: List<TypeError> = emptyList(),
)

interface NodeHandler {
    val kind: String

    fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): StatementCheck
}
