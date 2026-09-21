package printscript.typechecker.node

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack

data class Checked(
    val scope: ScopeStack,
    val error: TypeError? = null,
)

interface NodeHandler {
    val kind: String

    fun check(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Checked
}
