package printscript.support

import printscript.syntax.SyntaxNode

fun SyntaxNode.child(name: String): SyntaxNode =
    children.firstOrNull { it.name == name }
        ?: error("No child named '$name' in $this")

fun SyntaxNode.value(): String =
    token?.value?.orElse(null) ?: error("Node '$name' has no token value")

fun SyntaxNode.lhs(): SyntaxNode = children[0]

fun SyntaxNode.op(): String = children[1].value()

fun SyntaxNode.rhs(): SyntaxNode = children[2]
