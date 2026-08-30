package printscript.rule

import printscript.error.LintError
import printscript.syntax.SyntaxNode

interface LintRule {
    val name: String
    fun supports(node: SyntaxNode): Boolean
    fun check(node: SyntaxNode): List<LintError>
}
