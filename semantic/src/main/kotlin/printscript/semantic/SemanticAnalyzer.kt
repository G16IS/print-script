package printscript.semantic

import printscript.lexer.VariableType

class SemanticAnalyzer {
    private val symbolTable = SymbolTable()
    private val errors = mutableListOf<SemanticError>()

    //fun visitVariableDeclaration(Node: VariableDeclaration){
//      val initializer = node.initializer
//
// }

    // private fun inferSimpleType(exp: Expression): VariableType? = when (exp) {

    //}

    fun getErrors(): List<SemanticError> = errors
}