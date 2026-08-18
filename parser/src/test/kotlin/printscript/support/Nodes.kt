package printscript.support

import printscript.syntax.SyntaxNode

fun SyntaxNode.lhs(): SyntaxNode = children[0]

fun SyntaxNode.op(): String = children[1].value()

fun SyntaxNode.rhs(): SyntaxNode = children[2]
