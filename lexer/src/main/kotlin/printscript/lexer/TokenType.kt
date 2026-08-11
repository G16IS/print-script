package printscript.lexer

sealed class TokenType {
    override fun equals(other: Any?): Boolean = other != null && other::class == this::class
    override fun hashCode(): Int = this::class.hashCode()



    class Let : TokenType()
    class Identifier : TokenType()
    class NumberLiteral : TokenType()
    class StringLiteral : TokenType()
    class Operator : TokenType()
    class Semicolon : TokenType()
    class Assign : TokenType()
    class LeftParen : TokenType()
    class RightParen : TokenType()
    class Type : TokenType()
    class Call : TokenType()
    class Comma : TokenType()
    class Eof : TokenType()
}
