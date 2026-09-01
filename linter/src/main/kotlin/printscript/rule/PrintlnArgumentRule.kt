package printscript.rule

import printscript.error.InvalidPrintlnArgument
import printscript.error.LintError
import printscript.syntax.SyntaxNode

class PrintlnArgumentRule(
    val callee: String = "println",
) : LintRule {
    override val name: String
        get() = "println-argument-rule"

    override fun supports(node: SyntaxNode): Boolean {
        if (node.name != "call") return false
        val callToken =
            node
                .childOrNull("CALL")
                ?.token
                ?.value
                ?.orElse(null)
        return callToken == callee
    }

    override fun check(node: SyntaxNode): List<LintError> {
        val expr = node.childOrNull("expression")
        val unwrapped = expr?.let { unwrap(it) }

        if (unwrapped == null || unwrapped.name in SIMPLE_ARGUMENT_NODES) {
            return emptyList()
        }
        return listOf(
            InvalidPrintlnArgument(location = node.location),
        )
    }

    private fun unwrap(node: SyntaxNode): SyntaxNode {
        if (node.name == "group") {
            val innerValue = node.childOrNull("expression")
            if (innerValue != null) return unwrap(innerValue)
        }
        return if (node.children.size == 1) unwrap(node.children.first()) else node
    }

    companion object {
        private val SIMPLE_ARGUMENT_NODES = setOf("identifier", "number", "string")
    }
}
