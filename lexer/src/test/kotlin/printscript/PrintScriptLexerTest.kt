package printscript

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import printscript.error.UnexpectedToken
import printscript.support.PrintScriptLanguage
import printscript.support.assertLex
import printscript.support.assertTypes
import printscript.support.lex
import printscript.support.tok
import printscript.util.Result

/**
 * Tokenization of PrintScript v1 with the **test** [PrintScriptLanguage.ORDER]
 * (keywords last). Add a case here when a new lexeme should round-trip.
 */
class PrintScriptLexerTest {
    @Nested
    inner class Keywords {
        @Test
        fun `let is LET not ID`() = assertLex("let", tok("LET"), tok("EOF"))

        @Test
        fun `println is CALL and captures the lexeme`() = assertLex("println", tok("CALL", "println"), tok("EOF"))

        @Test
        fun `letter is an identifier that only starts like let`() = assertLex("letter", tok("ID", "letter"), tok("EOF"))

        @Test
        fun `printlnx is an identifier that only starts like println`() =
            assertLex("printlnx", tok("ID", "printlnx"), tok("EOF"))

        @Test
        fun `Let is not the keyword`() = assertLex("Let", tok("ID", "Let"), tok("EOF"))

        @Test
        fun `true is an identifier in v1`() = assertLex("true", tok("ID", "true"), tok("EOF"))
    }

    @Nested
    inner class Types {
        @Test
        fun `string is TYPE not ID`() = assertLex("string", tok("TYPE", "string"), tok("EOF"))

        @Test
        fun `number is TYPE not ID`() = assertLex("number", tok("TYPE", "number"), tok("EOF"))

        @Test
        fun `stringy is an identifier`() = assertLex("stringy", tok("ID", "stringy"), tok("EOF"))
    }

    @Nested
    inner class Identifiers {
        @Test
        fun `single letter`() = assertLex("x", tok("ID", "x"), tok("EOF"))

        @Test
        fun `underscore prefix`() = assertLex("_x", tok("ID", "_x"), tok("EOF"))

        @Test
        fun `digits after the first character`() = assertLex("a1", tok("ID", "a1"), tok("EOF"))

        @Test
        fun `camelCase`() = assertLex("lastName", tok("ID", "lastName"), tok("EOF"))

        @Test
        fun `snake_case`() = assertLex("last_name", tok("ID", "last_name"), tok("EOF"))
    }

    @Nested
    inner class NumberLiterals {
        @Test
        fun `zero`() = assertLex("0", tok("NUMBER_LITERAL", "0"), tok("EOF"))

        @Test
        fun `integer`() = assertLex("42", tok("NUMBER_LITERAL", "42"), tok("EOF"))

        @Test
        fun `leading zeros stay one literal`() = assertLex("007", tok("NUMBER_LITERAL", "007"), tok("EOF"))

        @Test
        fun `decimal`() = assertLex("1.5", tok("NUMBER_LITERAL", "1.5"), tok("EOF"))

        @Test
        fun `decimal starting at zero`() = assertLex("0.0", tok("NUMBER_LITERAL", "0.0"), tok("EOF"))

        @Test
        fun `minus is an operator not part of the number`() =
            assertLex(
                "-1",
                tok("OPERATOR", "-"),
                tok("NUMBER_LITERAL", "1"),
                tok("EOF"),
            )

        @Test
        fun `digits then letters split into number and identifier`() =
            assertLex(
                "123abc",
                tok("NUMBER_LITERAL", "123"),
                tok("ID", "abc"),
                tok("EOF"),
            )

        @Test
        fun `scientific notation is not a single literal`() =
            assertLex(
                "1e10",
                tok("NUMBER_LITERAL", "1"),
                tok("ID", "e10"),
                tok("EOF"),
            )

        @Test
        fun `a trailing dot is not a valid number literal`() {
            // `1.` sólo matchea el `partial` de NUMBER_LITERAL, nunca el matcher
            // completo: un match parcial no alcanza para emitir un token.
            val result = lex("1.;")
            assertTrue(result is Result.Err && result.error is UnexpectedToken)
        }
    }

    @Nested
    inner class StringLiterals {
        @Test
        fun `empty string keeps the quotes`() = assertLex("\"\"", tok("STRING_LITERAL", "\"\""), tok("EOF"))

        @Test
        fun `hello keeps the quotes`() = assertLex("\"hello\"", tok("STRING_LITERAL", "\"hello\""), tok("EOF"))

        @Test
        fun `spaces inside the quotes are part of the value`() =
            assertLex("\"hello world\"", tok("STRING_LITERAL", "\"hello world\""), tok("EOF"))

        @Test
        fun `string of a single space`() = assertLex("\" \"", tok("STRING_LITERAL", "\" \""), tok("EOF"))

        @Test
        fun `the first closing quote ends the string`() =
            assertLex(
                "\"hello\"world",
                tok("STRING_LITERAL", "\"hello\""),
                tok("ID", "world"),
                tok("EOF"),
            )

        @Test
        fun `single quoted hello keeps the quotes`() =
            assertLex("'hello'", tok("STRING_LITERAL", "'hello'"), tok("EOF"))

        @Test
        fun `empty single quoted string keeps the quotes`() = assertLex("''", tok("STRING_LITERAL", "''"), tok("EOF"))

        @Test
        fun `double quotes are allowed inside single quotes`() =
            assertLex("'say \"hi\"'", tok("STRING_LITERAL", "'say \"hi\"'"), tok("EOF"))
    }

    @Nested
    inner class OperatorsAndPunctuation {
        @Test
        fun `plus`() = assertLex("+", tok("OPERATOR", "+"), tok("EOF"))

        @Test
        fun `minus`() = assertLex("-", tok("OPERATOR", "-"), tok("EOF"))

        @Test
        fun `star`() = assertLex("*", tok("OPERATOR", "*"), tok("EOF"))

        @Test
        fun `slash`() = assertLex("/", tok("OPERATOR", "/"), tok("EOF"))

        @Test
        fun `punctuation does not capture the lexeme`() =
            assertLex(
                ":=;(),",
                tok("COLON"),
                tok("ASSIGN"),
                tok("SEMICOLON"),
                tok("LEFT_PAREN"),
                tok("RIGHT_PAREN"),
                tok("COMMA"),
                tok("EOF"),
            )

        @Test
        fun `two minuses are two operators`() =
            assertLex(
                "--",
                tok("OPERATOR", "-"),
                tok("OPERATOR", "-"),
                tok("EOF"),
            )

        @Test
        fun `juxtaposed operands and operators`() =
            assertLex(
                "1+2*3",
                tok("NUMBER_LITERAL", "1"),
                tok("OPERATOR", "+"),
                tok("NUMBER_LITERAL", "2"),
                tok("OPERATOR", "*"),
                tok("NUMBER_LITERAL", "3"),
                tok("EOF"),
            )
    }

    @Nested
    inner class Statements {
        @Test
        fun `string declaration`() =
            assertLex(
                "let x: string = \"hello\";",
                tok("LET"),
                tok("ID", "x"),
                tok("COLON"),
                tok("TYPE", "string"),
                tok("ASSIGN"),
                tok("STRING_LITERAL", "\"hello\""),
                tok("SEMICOLON"),
                tok("EOF"),
            )

        @Test
        fun `number declaration`() =
            assertLex(
                "let pepa: number = 42;",
                tok("LET"),
                tok("ID", "pepa"),
                tok("COLON"),
                tok("TYPE", "number"),
                tok("ASSIGN"),
                tok("NUMBER_LITERAL", "42"),
                tok("SEMICOLON"),
                tok("EOF"),
            )

        @Test
        fun `decimal declaration`() =
            assertLex(
                "let a: number = 1.5;",
                tok("LET"),
                tok("ID", "a"),
                tok("COLON"),
                tok("TYPE", "number"),
                tok("ASSIGN"),
                tok("NUMBER_LITERAL", "1.5"),
                tok("SEMICOLON"),
                tok("EOF"),
            )

        @Test
        fun `println of concatenated identifiers`() =
            assertLex(
                "println(name + \" \" + lastName);",
                tok("CALL", "println"),
                tok("LEFT_PAREN"),
                tok("ID", "name"),
                tok("OPERATOR", "+"),
                tok("STRING_LITERAL", "\" \""),
                tok("OPERATOR", "+"),
                tok("ID", "lastName"),
                tok("RIGHT_PAREN"),
                tok("SEMICOLON"),
                tok("EOF"),
            )

        @Test
        fun `println of an arithmetic expression`() =
            assertLex(
                "println(1 + 2 * 3);",
                tok("CALL", "println"),
                tok("LEFT_PAREN"),
                tok("NUMBER_LITERAL", "1"),
                tok("OPERATOR", "+"),
                tok("NUMBER_LITERAL", "2"),
                tok("OPERATOR", "*"),
                tok("NUMBER_LITERAL", "3"),
                tok("RIGHT_PAREN"),
                tok("SEMICOLON"),
                tok("EOF"),
            )

        @Test
        fun `println with a comma between arguments`() =
            assertLex(
                "println(1, 2);",
                tok("CALL", "println"),
                tok("LEFT_PAREN"),
                tok("NUMBER_LITERAL", "1"),
                tok("COMMA"),
                tok("NUMBER_LITERAL", "2"),
                tok("RIGHT_PAREN"),
                tok("SEMICOLON"),
                tok("EOF"),
            )

        @Test
        fun `several statements stay one token stream`() =
            assertTypes(
                "let x: number = 1;\nprintln(x);",
                "LET",
                "ID",
                "COLON",
                "TYPE",
                "ASSIGN",
                "NUMBER_LITERAL",
                "SEMICOLON",
                "CALL",
                "LEFT_PAREN",
                "ID",
                "RIGHT_PAREN",
                "SEMICOLON",
                "EOF",
            )
    }

    @Nested
    inner class CategoryOrder {
        @Test
        fun `keywords first makes let a LET`() = assertTypes("let", "LET", "EOF")

        @Test
        fun `reversed order makes let an identifier`() =
            assertTypes("let", PrintScriptLanguage.reversedOrder(), "ID", "EOF")

        @Test
        fun `operators still tokenize when nothing else matches`() =
            assertLex(
                "+",
                tok("OPERATOR", "+"),
                tok("EOF"),
            )
    }
}
