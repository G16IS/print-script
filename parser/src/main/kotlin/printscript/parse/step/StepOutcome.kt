package printscript.parse.step

import printscript.error.ParserError
import printscript.syntax.Location
import printscript.syntax.SyntaxNode

sealed interface StepOutcome {
    data class Hit(
        val node: SyntaxNode? = null,
        val location: Location? = node?.location,
    ) : StepOutcome

    data object Miss : StepOutcome

    data class Failed(
        val error: ParserError,
    ) : StepOutcome
}
