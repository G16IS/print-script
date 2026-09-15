package printscript.node

/** Grammar rule names and captured seq children the interpreter walks. */
internal object AstNames {
    const val ID = "ID"
    const val TYPE = "TYPE"
    const val VARIABLE = "variable"
    const val CONSTANT = "constant"
    const val ASSIGNMENT = "assignment"
    const val IF = "if"
    const val BLOCK = "block"
    const val STATEMENTS = "statements"
    const val ELSE_CLAUSE = "else-clause"
    const val EXPRESSION_STMT = "expression-stmt"
    const val EXPRESSION = "expression"
    const val TERM = "term"
    const val NUMBER = "number"
    const val STRING = "string"
    const val BOOLEAN = "boolean"
    const val IDENTIFIER = "identifier"
    const val CALL = "call"
    const val GROUP = "group"
}
