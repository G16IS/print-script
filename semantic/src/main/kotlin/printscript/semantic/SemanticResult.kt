package printscript.semantic

import printscript.common.ast.Program

sealed interface SemanticResult {
    data class Success(
        val program: Program,
    ) : SemanticResult

    data class Failure(
        val errors: List<SemanticError>,
    ) : SemanticResult
}
