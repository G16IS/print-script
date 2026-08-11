package printscript

import printscript.ast.Program

interface SemanticAnalyzer {
    fun analyze(program: Program): SemanticResult
}

sealed interface SemanticResult {
    data class Success(
        val program: Program,
    ) : SemanticResult

    data class Failure(
        val errors: List<SemanticError>,
    ) : SemanticResult
}
