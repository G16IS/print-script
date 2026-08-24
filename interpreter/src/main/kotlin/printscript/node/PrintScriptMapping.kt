package printscript.node

/**
 * Default mapping from grammar rule names (as produced by grammar.config.json v1)
 * to the interpreter's closed vocabulary.
 */
object PrintScriptMapping {
    val mapping: Map<String, NodeKind> =
        mapOf(
            "variable" to NodeKind.VARIABLE_DECLARATION,
            "expression-stmt" to NodeKind.EXPRESSION_STMT,
            "expression" to NodeKind.BINARY_OP,
            "term" to NodeKind.BINARY_OP,
            "number" to NodeKind.NUMBER_LITERAL,
            "string" to NodeKind.STRING_LITERAL,
            "identifier" to NodeKind.IDENTIFIER,
            "call" to NodeKind.CALL,
            "group" to NodeKind.GROUP,
        )
}
