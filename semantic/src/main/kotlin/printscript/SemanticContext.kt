package printscript

data class SemanticContext(
    val symbolTable: SymbolTable = SymbolTable(),
    val errors: MutableList<SemanticError> = mutableListOf(),
)
