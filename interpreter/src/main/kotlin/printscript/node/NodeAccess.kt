package printscript.node

import printscript.error.RuntimeError
import printscript.error.UnrecognizedNode
import printscript.syntax.SyntaxNode
import printscript.util.Result

internal fun SyntaxNode.tokenValue(): Result<String, RuntimeError> {
    val text = token?.value?.orElse(null)
    return if (text != null) {
        Result.Ok(text)
    } else {
        Result.Err(UnrecognizedNode(name, location))
    }
}

internal fun SyntaxNode.childAt(index: Int): Result<SyntaxNode, RuntimeError> = nodeOrErr(children.getOrNull(index))

internal fun SyntaxNode.firstChild(): Result<SyntaxNode, RuntimeError> = childAt(0)

internal fun SyntaxNode.namedChild(name: String): Result<SyntaxNode, RuntimeError> = nodeOrErr(childOrNull(name))

private fun SyntaxNode.nodeOrErr(child: SyntaxNode?): Result<SyntaxNode, RuntimeError> =
    child?.let { Result.Ok(it) } ?: Result.Err(UnrecognizedNode(name, location))
