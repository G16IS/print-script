package lexer

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Test
import printscript.common.reader.CharPosition
import printscript.lexer.Assign
import printscript.lexer.Call
import printscript.lexer.Identifier
import printscript.lexer.Let
import printscript.lexer.TokenStream
import printscript.lexer.Semicolon
import printscript.lexer.StringLiteral
import printscript.lexer.Token
import printscript.lexer.TokenType
import printscript.lexer.Type
import java.util.Optional
import kotlin.test.assertEquals

class TokenizeTest {
    @Test
    fun tokenizeVariableAssignmentTest() {
        val statement: String = "let x: string = 'hello';"
        val expectedTokens: List<Token> = expectedTokens()
        val tokenStream = TokenStream(MockReader(statement), makeMap())
        val tokenLister = TokenLister()
        assertEquals(expectedTokens, tokenLister.listTokens(tokenStream))
    }

    private fun makeMap(): PersistentMap<String, TokenType>{
        return persistentMapOf(
            "let" to Let(),
            "println" to Call()
        )
    }

    private fun expectedTokens(): List<Token> {
        return listOf(
            Token(Let(), Optional.empty(), CharPosition(0, 0), CharPosition(0, 2)),
            Token(Identifier(), Optional.of("x"), CharPosition(0, 4), CharPosition(0, 5)),
            Token(Type(), Optional.of("string"),  CharPosition(0, 5), CharPosition(0, 12)),
            Token(Assign(), Optional.empty(),  CharPosition(0, 14), CharPosition(0, 15)),
            Token(StringLiteral(), Optional.of("hello"),  CharPosition(0, 16), CharPosition(0, 23)),
            Token(Semicolon(), Optional.empty(),  CharPosition(0, 23), CharPosition(0, 24))
        )
    }
}
