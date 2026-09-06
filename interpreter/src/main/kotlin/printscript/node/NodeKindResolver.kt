package printscript.node

import printscript.error.NoNodeKindForNode
import printscript.error.RuntimeError
import printscript.syntax.SyntaxNode
import printscript.util.Result

class NodeKindResolver(
    private val mapping: Map<String, NodeKind>,
) {
    fun resolve(node: SyntaxNode): Result<NodeKind, RuntimeError> {
        val kind: NodeKind? = mapping[node.name]

        return if (kind != null) Result.Ok(kind) else Result.Err(NoNodeKindForNode(node.name, node.location))
    }
}
