package lexer

import org.junit.Test
import printscript.reader.CharPosition
import printscript.DefaultLexerFactory
import printscript.Lexer
import printscript.domain.Token
import printscript.domain.TokenType
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
            Token(TokenType.LET, Optional.empty(), CharPosition(0, 1), CharPosition(0, 3)),
            Token(TokenType.IDENTIFIER, Optional.of("x"), CharPosition(0, 5), CharPosition(0, 5)),
            Token(TokenType.COLON, Optional.empty(), CharPosition(0, 6), CharPosition(0, 6)),
            Token(TokenType.TYPE, Optional.of("string"), CharPosition(0, 8), CharPosition(0, 13)),
            Token(TokenType.ASSIGN, Optional.empty(), CharPosition(0, 15), CharPosition(0, 15)),
            Token(TokenType.STRING_LITERAL, Optional.of("hello"), CharPosition(0, 17), CharPosition(0, 23)),
            Token(TokenType.SEMICOLON, Optional.empty(), CharPosition(0, 24), CharPosition(0, 24)),
            Token(TokenType.EOF, Optional.empty(), CharPosition(0, 24), CharPosition(0, 24))
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
            Token(TokenType.CALL, Optional.of("println"), CharPosition(0, 1), CharPosition(0, 7)),
            Token(TokenType.LEFT_PAREN, Optional.empty(), CharPosition(0, 8), CharPosition(0, 8)),
            Token(TokenType.IDENTIFIER, Optional.of("name"), CharPosition(0, 9), CharPosition(0, 12)),
            Token(TokenType.OPERATOR, Optional.of("+"), CharPosition(0, 14), CharPosition(0, 14)),
            Token(TokenType.STRING_LITERAL, Optional.of(" "), CharPosition(0, 16), CharPosition(0, 18)),
            Token(TokenType.OPERATOR, Optional.of("+"), CharPosition(0, 20), CharPosition(0, 20)),
            Token(TokenType.IDENTIFIER, Optional.of("lastName"), CharPosition(0, 22), CharPosition(0, 29)),
            Token(TokenType.RIGHT_PAREN, Optional.empty(), CharPosition(0, 30), CharPosition(0, 30)),
            Token(TokenType.SEMICOLON, Optional.empty(), CharPosition(0, 31), CharPosition(0, 31)),
            Token(TokenType.EOF, Optional.empty(), CharPosition(0, 31), CharPosition(0, 31)),
        )
    }
}
