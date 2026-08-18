package printscript.syntax

import printscript.ast.Location
import printscript.domain.Token

data class SyntaxNode(
    val name: String,
    val token: Token? = null,
    val children: List<SyntaxNode> = emptyList(),
    val location: Location
)
