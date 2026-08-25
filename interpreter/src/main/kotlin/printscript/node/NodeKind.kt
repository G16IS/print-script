package printscript.node

enum class NodeKind {
    VARIABLE_DECLARATION,
    EXPRESSION_STMT,
    BINARY_OP,
    NUMBER_LITERAL,
    STRING_LITERAL,
    IDENTIFIER,
    CALL,
    GROUP,
}
