package lexer

import org.junit.Test
import printscript.reader.CharPosition
import printscript.Lexer
import printscript.ast.Location
import printscript.domain.Token
import java.util.Optional
import kotlin.test.assertEquals

class TokenizeTest {
    @Test
    fun tokenizeVariableAssignmentTest() {
        val statement: String = "let x: string = \"hello\";"
        val expectedTokens: List<Token> = expectedTokens()
        val lexer: Lexer = MockLexerFactory.create(statement)
        val tokenLister = TokenLister()
        assertEquals(expectedTokens, tokenLister.listTokens(lexer))
    }

    private fun expectedTokens(): List<Token> {
        return listOf(
            Token("LET", Optional.empty(), Location(CharPosition(0, 1), CharPosition(0, 3))),
            Token("IDENTIFIER", Optional.of("x"), Location( CharPosition(0, 5), CharPosition(0, 5))),
            Token("COLON", Optional.empty(), Location(CharPosition(0, 6), CharPosition(0, 6))),
            Token("TYPE", Optional.of("string"), Location(CharPosition(0, 8), CharPosition(0, 13))),
            Token("ASSIGN", Optional.empty(), Location(CharPosition(0, 15), CharPosition(0, 15))),
            Token("STRING_LITERAL", Optional.of("hello"), Location(CharPosition(0, 17), CharPosition(0, 23))),
            Token("SEMICOLON", Optional.empty(), Location(CharPosition(0, 24), CharPosition(0, 24))),
            Token("EOF", Optional.empty(), Location(CharPosition(0, 24), CharPosition(0, 24)))
        )
    }

    @Test
    fun tokenizePrintStatementTest() {
        val statement: String = "println(name + \" \" + lastName);"
        val expectedTokens: List<Token> = expectedTokens1()
        val lexer: Lexer = MockLexerFactory.create(statement)
        val tokenLister = TokenLister()
        assertEquals(expectedTokens, tokenLister.listTokens(lexer))
    }

    private fun expectedTokens1(): List<Token> {
        return listOf(
            Token("CALL", Optional.of("println"), Location(CharPosition(0, 1), CharPosition(0, 7))),
            Token("LEFT_PAREN", Optional.empty(), Location(CharPosition(0, 8), CharPosition(0, 8))),
            Token("IDENTIFIER", Optional.of("name"), Location(CharPosition(0, 9), CharPosition(0, 12))),
            Token("OPERATOR", Optional.of("+"), Location(CharPosition(0, 14), CharPosition(0, 14))),
            Token("STRING_LITERAL", Optional.of(" "), Location(CharPosition(0, 16), CharPosition(0, 18))),
            Token("OPERATOR", Optional.of("+"), Location(CharPosition(0, 20), CharPosition(0, 20))),
            Token("IDENTIFIER", Optional.of("lastName"), Location(CharPosition(0, 22), CharPosition(0, 29))),
            Token("RIGHT_PAREN", Optional.empty(), Location(CharPosition(0, 30), CharPosition(0, 30))),
            Token("SEMICOLON", Optional.empty(), Location(CharPosition(0, 31), CharPosition(0, 31))),
            Token("EOF", Optional.empty(), Location(CharPosition(0, 31), CharPosition(0, 31))),
        )
    }
}
