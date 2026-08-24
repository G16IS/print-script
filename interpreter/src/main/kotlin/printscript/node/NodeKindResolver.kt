package printscript.node

import printscript.error.NoNodeKindForNode
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.util.Result

class NodeKindResolver(
    val mapping: Map<String, NodeKind>,
) {
    fun resolve(node: SyntaxNode): Result<NodeKind, TypeError> {
        val kind: NodeKind? = mapping[node.name]

        return if (kind != null) Result.Ok(kind) else Result.Err(NoNodeKindForNode(node.name, node.location))
    }
}
