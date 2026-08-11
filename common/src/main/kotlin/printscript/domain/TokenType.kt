package printscript.domain

/**
 * Lexical categories of the language.
 */
enum class TokenType {
    // Keywords / declarations
    LET,
    TYPE,
    CALL,

    // Literals and identifiers
    IDENTIFIER,
    NUMBER_LITERAL,
    STRING_LITERAL,

    // Operators and punctuation
    OPERATOR,
    ASSIGN,
    COLON,
    SEMICOLON,
    COMMA,
    LEFT_PAREN,
    RIGHT_PAREN,

    // Stream control
    EOF,
}
