package printscript.util

import printscript.ast.Location
import printscript.domain.Token
import printscript.syntax.SyntaxNode

fun tokenLeaf(token: Token): SyntaxNode =
    SyntaxNode(name = token.type, token = token, location = token.location)

fun wrap(name: String, child: SyntaxNode): SyntaxNode =
    SyntaxNode(name = name, children = listOf(child), location = child.location)

fun binary(name: String, left: SyntaxNode, op: Token, right: SyntaxNode): SyntaxNode =
    SyntaxNode(
        name = name,
        children = listOf(left, tokenLeaf(op), right),
        location = Location(left.location.start, right.location.end)
    )
