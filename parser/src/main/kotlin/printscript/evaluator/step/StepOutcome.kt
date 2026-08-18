package printscript.evaluator.step

import printscript.ast.Location
import printscript.syntax.SyntaxNode

data class StepOutcome(
    val matched: Boolean,
    val node: SyntaxNode? = null,
    val location: Location? = null
) {
    companion object {
        fun miss(): StepOutcome = StepOutcome(matched = false)

        fun hit(node: SyntaxNode? = null, location: Location? = node?.location): StepOutcome =
            StepOutcome(matched = true, node = node, location = location)
    }
}
