package printscript.node

/** Grammar rule names and captured seq children the interpreter walks. */
internal object AstNames {
    const val ID = "ID"
    const val VARIABLE = "variable"
    const val ASSIGNMENT = "assignment"
    const val EXPRESSION_STMT = "expression-stmt"
    const val EXPRESSION = "expression"
    const val TERM = "term"
    const val NUMBER = "number"
    const val STRING = "string"
    const val IDENTIFIER = "identifier"
    const val CALL = "call"
    const val GROUP = "group"
}
