package edu.austral.dissis.testing.ast

import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram

object AstRenderer {
    fun program(program: SyntaxProgram): String =
        program.statements.joinToString("\n") { node(it) }

    fun specs(specs: List<AstSpec>): String =
        specs.joinToString("\n") { spec(it) }

    fun node(node: SyntaxNode, depth: Int = 0): String {
        val line = indent(depth) + label(node)
        if (node.children.isEmpty()) return line
        return line + "\n" + node.children.joinToString("\n") { node(it, depth + 1) }
    }

    fun spec(spec: AstSpec, depth: Int = 0): String {
        val line = indent(depth) + label(spec)
        if (spec.children.isEmpty()) return line
        return line + "\n" + spec.children.joinToString("\n") { spec(it, depth + 1) }
    }

    private fun label(node: SyntaxNode): String {
        val value = node.token?.value?.orElse(null) ?: return node.name
        return "${node.name} $value"
    }

    private fun label(spec: AstSpec): String {
        val value = spec.value ?: return spec.name
        return "${spec.name} $value"
    }

    private fun indent(depth: Int): String = "  ".repeat(depth)
}
