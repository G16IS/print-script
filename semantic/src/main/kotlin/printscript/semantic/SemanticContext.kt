package printscript.semantic

data class SemanticContext(
    val symbolTable: SymbolTable = SymbolTable(),
    val errors: MutableList<SemanticError> = mutableListOf(),
)