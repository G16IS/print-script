package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.ExactRule
import printscript.domain.LanguageConfig
import printscript.support.MockReader
import printscript.support.PrintScriptLanguage
import printscript.support.assertLex
import printscript.support.assertLocation
import printscript.support.assertTypes
import printscript.support.castTokenResult
import printscript.support.lex
import printscript.support.lexer
import printscript.support.tok

class TokenStreamTest {
    @Nested
    inner class WhitespaceAndEof {
        @Test
        fun `empty source is a single EOF`() = assertTypes("", "EOF")

        @Test
        fun `spaces only are a single EOF`() = assertTypes("   ", "EOF")

        @Test
        fun `tabs and newlines only are a single EOF`() = assertTypes("\t\n\r\n", "EOF")

        @Test
        fun `leading and trailing whitespace is skipped`() = assertLex("  let  ", tok("LET"), tok("EOF"))

        @Test
        fun `newlines between tokens are skipped`() = assertTypes("let\nx", "LET", "ID", "EOF")

        @Test
        fun `tabs between tokens are skipped`() = assertTypes("let\tx", "LET", "ID", "EOF")

        @Test
        fun `nextToken after EOF yields another EOF`() {
            val stream = lexer("")
            assertEquals("EOF", castTokenResult(stream.nextToken()).type)
            assertEquals("EOF", castTokenResult(stream.nextToken()).type)
        }
    }

    @Nested
    inner class Peek {
        @Test
        fun `peek null does not consume`() {
            val stream = lexer("let x")
            assertEquals("LET", stream.peek(null).type)
            assertEquals("LET", stream.peek(null).type)
            assertEquals("LET", stream.nextToken().type)
        }

        @Test
        fun `peek zero is the same as peek null`() {
            val stream = lexer("let")
            assertEquals(stream.peek(null), stream.peek(0))
        }

        @Test
        fun `peek offset looks ahead without consuming`() {
            val stream = lexer("let x;")
            assertEquals("LET", stream.peek(0).type)
            assertEquals("ID", stream.peek(1).type)
            assertEquals("SEMICOLON", stream.peek(2).type)
            assertEquals("EOF", stream.peek(3).type)
            assertEquals("LET", stream.nextToken().type)
            assertEquals("ID", stream.nextToken().type)
        }

        @Test
        fun `peek then next then peek resume in order`() {
            val stream = lexer("1 + 2")
            assertEquals("NUMBER_LITERAL", stream.peek(null).type)
            assertEquals("1", stream.nextToken().value.get())
            assertEquals("OPERATOR", stream.peek(null).type)
            assertEquals("+", stream.nextToken().value.get())
            assertEquals("NUMBER_LITERAL", stream.peek(null).type)
        }

        @Test
        fun `peek on empty source is EOF`() {
            assertEquals("EOF", lexer("").peek(null).type)
        }
    }

    @Nested
    inner class Errors {
        @Test
        fun `unexpected character`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    lex("@")
                }
            assertTrue(exception.message!!.contains("Unexpected token"))
        }

        @Test
        fun `single quote is not a string delimiter`() {
            assertThrows<IllegalArgumentException> { lex("'hi'") }
        }

        @Test
        fun `unterminated string`() {
            val exception =
                assertThrows<IllegalStateException> {
                    lex("\"hello")
                }
            assertTrue(exception.message!!.contains("Unexpected end of file"))
        }

        @Test
        fun `unterminated string after other tokens`() {
            assertThrows<IllegalStateException> { lex("let x = \"hello") }
        }

        @Test
        fun `trailing dot of a number at EOF is still partial`() {
            val exception =
                assertThrows<IllegalStateException> {
                    lex("1.")
                }
            assertTrue(exception.message!!.contains("Unexpected end of file"))
        }
    }

    @Nested
    inner class Locations {
        @Test
        fun `string declaration locations match MockReader columns`() {
            val tokens = lex("let x: string = \"hello\";")
            assertLocation(tokens[0], startCol = 1, endCol = 3)
            assertLocation(tokens[1], startCol = 5, endCol = 5)
            assertLocation(tokens[2], startCol = 6, endCol = 6)
            assertLocation(tokens[3], startCol = 8, endCol = 13)
            assertLocation(tokens[4], startCol = 15, endCol = 15)
            assertLocation(tokens[5], startCol = 17, endCol = 23)
            assertLocation(tokens[6], startCol = 24, endCol = 24)
            assertLocation(tokens[7], startCol = 24, endCol = 24)
        }

        @Test
        fun `EOF on empty input sits at column zero`() {
            assertLocation(lex("")[0], startCol = 0, endCol = 0)
        }

        @Test
        fun `MockReader does not bump the line on newline`() {
            val tokens = lex("let\nx")
            assertLocation(tokens[0], startCol = 1, endCol = 3)
            assertLocation(tokens[1], startCol = 5, endCol = 5)
        }
    }

    @Nested
    inner class Engine {
        @Test
        fun `empty language config treats any character as unexpected`() {
            val config = LanguageConfig(emptyList(), emptyMap())
            assertThrows<IllegalArgumentException> { lex("a", config) }
        }

        @Test
        fun `narrow number partial never reaches the digits after the dot`() {
            val config =
                PrintScriptLanguage.config(
                    numberPartial = PrintScriptLanguage.PRODUCTION_NUMBER_PARTIAL,
                )
            val exception = assertThrows<IllegalArgumentException> { lex("1.5", config) }
            assertTrue(exception.message!!.contains("Unexpected token"))
        }

        @Test
        fun `narrow string partial never reaches the closing quote`() {
            val config =
                PrintScriptLanguage.config(
                    stringPartial = PrintScriptLanguage.PRODUCTION_STRING_PARTIAL,
                )
            assertThrows<IllegalStateException> { lex("\"hello\"", config) }
        }

        @Test
        fun `greedy exact match consumes the longest prefix that stays valid or partial`() {
            val ifRule = ExactRule(listOf("if"), "IF", false)
            val config =
                LanguageConfig(
                    order = listOf("keywords"),
                    config = mapOf("keywords" to listOf(ifRule)),
                )
            assertTypes("if", config, "IF", "EOF")
        }

        @Test
        fun `prefix operators in the same category collide when emitting the shorter one`() {
            val assign = ExactRule(listOf("="), "ASSIGN", false)
            val equals = ExactRule(listOf("=="), "EQUALS", false)
            val config =
                LanguageConfig(
                    order = listOf("operators"),
                    config = mapOf("operators" to listOf(assign, equals)),
                )
            assertThrows<IllegalArgumentException> { lex("=x", config) }
            assertLex("==", config, tok("EQUALS"), tok("EOF"))
        }
    }

    @Nested
    inner class Factory {
        @Test
        fun `DefaultLexerFactory builds a stream that tokenizes PrintScript`() {
            val stream =
                DefaultLexerFactory.create(
                    MockReader("let"),
                    PrintScriptLanguage.config(),
                )
            assertEquals("LET", stream.nextToken().type)
            assertEquals("EOF", stream.nextToken().type)
        }
    }
}
