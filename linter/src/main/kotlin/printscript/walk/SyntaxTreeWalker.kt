package printscript.walk

import printscript.syntax.SyntaxNode

object SyntaxTreeWalker {
    fun walk(node: SyntaxNode): List<SyntaxNode> {
        val result = mutableListOf<SyntaxNode>()
        collect(node, result)
        return result
    }

    private fun collect(
        node: SyntaxNode,
        accumulator: MutableList<SyntaxNode>,
    ) {
        accumulator.add(node)
        for (child in node.children) {
            collect(child, accumulator)
        }
    }
}
