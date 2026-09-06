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

internal fun SyntaxNode.childAt(index: Int): Result<SyntaxNode, RuntimeError> {
    val child = children.getOrNull(index)
    return if (child != null) {
        Result.Ok(child)
    } else {
        Result.Err(UnrecognizedNode(name, location))
    }
}
