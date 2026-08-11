package lexer

import org.junit.Test
import printscript.common.reader.CharPosition
import printscript.factory.DefaultLexerFactory
import printscript.lexer.Lexer
import printscript.lexer.Token
import printscript.lexer.TokenType
import java.util.Optional
import kotlin.test.assertEquals

class TokenizeTest {
    @Test
    fun tokenizeVariableAssignmentTest() {
        val statement: String = "let x: string = 'hello';"
        val expectedTokens: List<Token> = expectedTokens()
        val lexer: Lexer = DefaultLexerFactory.create(MockReader(statement))
        val tokenLister = TokenLister()
        assertEquals(expectedTokens, tokenLister.listTokens(lexer))
    }

    private fun expectedTokens(): List<Token> {
        return listOf(
            Token(TokenType.Let(), Optional.empty(), CharPosition(0, 1), CharPosition(0, 3)),
            Token(TokenType.Identifier(), Optional.of("x"), CharPosition(0, 5), CharPosition(0, 5)),
            Token(TokenType.Type(), Optional.of("string"),  CharPosition(0, 6), CharPosition(0, 13)),
            Token(TokenType.Assign(), Optional.empty(),  CharPosition(0, 15), CharPosition(0, 15)),
            Token(TokenType.StringLiteral(), Optional.of("hello"),  CharPosition(0, 17), CharPosition(0, 23)),
            Token(TokenType.Semicolon(), Optional.empty(),  CharPosition(0, 24), CharPosition(0, 24)),
            Token(TokenType.Eof(), Optional.empty(),  CharPosition(0, 24), CharPosition(0, 24))
        )
    }

    @Test
    fun tokenizePrintStatementTest() {
        val statement: String = "println(name + \" \" + lastName);"
        val expectedTokens: List<Token> = expectedTokens1()
        val lexer: Lexer = DefaultLexerFactory.create(MockReader(statement))
        val tokenLister = TokenLister()
        assertEquals(expectedTokens, tokenLister.listTokens(lexer))
    }

    private fun expectedTokens1(): List<Token> {
        return listOf(
            Token(TokenType.Call(), Optional.of("println"), CharPosition(0, 1), CharPosition(0, 7)),
            Token(TokenType.LeftParen(), Optional.empty(), CharPosition(0, 8), CharPosition(0, 8)),
            Token(TokenType.Identifier(), Optional.of("name"),  CharPosition(0, 9), CharPosition(0, 12)),
            Token(TokenType.Operator(), Optional.of("+"),  CharPosition(0, 14), CharPosition(0, 14)),
            Token(TokenType.StringLiteral(), Optional.of(" "),  CharPosition(0, 16), CharPosition(0, 18)),
            Token(TokenType.Operator(), Optional.of("+"),  CharPosition(0, 20), CharPosition(0, 20)),
            Token(TokenType.Identifier(), Optional.of("lastName"),  CharPosition(0, 22), CharPosition(0, 29)),
            Token(TokenType.RightParen(), Optional.empty(), CharPosition(0, 30), CharPosition(0, 30)),
            Token(TokenType.Semicolon(), Optional.empty(), CharPosition(0, 31), CharPosition(0, 31)),
            Token(TokenType.Eof(), Optional.empty(), CharPosition(0, 31), CharPosition(0, 31)),
        )
    }
}
