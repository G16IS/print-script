package printscript.syntax

import printscript.ast.Location
import printscript.domain.Token

/**
 * Generic syntax tree. Rule names come from the grammar; there is no typed AST.
 * Semantic analysis should walk this tree.
 */
data class SyntaxNode(
    val name: String,
    val token: Token? = null,
    val children: List<SyntaxNode> = emptyList(),
    val location: Location,
) {
    fun childOrNull(name: String): SyntaxNode? = children.firstOrNull { it.name == name }

    fun child(name: String): SyntaxNode = childOrNull(name) ?: error("No child named '$name' in '${this.name}'")

    fun findOrNull(name: String): SyntaxNode? {
        if (this.name == name) return this
        return children.firstNotNullOfOrNull { it.findOrNull(name) }
    }

    fun find(name: String): SyntaxNode = findOrNull(name) ?: error("No descendant named '$name' in '${this.name}'")

    fun value(): String = token?.value?.orElse(null) ?: error("Node '$name' has no token value")
}
