package printscript

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import printscript.domain.LinterConfig
import printscript.domain.RuleConfig
import printscript.error.InvalidIdentifierFormat
import printscript.error.InvalidPrintlnArgument
import printscript.factory.DefaultLinterFactory
import printscript.infrastructure.reader.JSONLinterConfigReader
import printscript.support.LinterPsSupport

class LinterIntegrationTest {
    private val defaultConfig =
        JSONLinterConfigReader.read(
            checkNotNull(javaClass.getResourceAsStream("/linter.config.json")) {
                "Missing linter.config.json"
            },
        )

    @Test
    fun `clean code produces isOk Report with default config`() {
        val code =
            """
            let userName: string = "Alice";
            let userAge: number = 25;
            println(userName);
            println(userAge);
            """.trimIndent()

        val program = LinterPsSupport.parse(code)
        val linter = DefaultLinterFactory.create(defaultConfig)
        val report = linter.lint(program)

        assertTrue(report.isOk)
        assertTrue(report.errors.isEmpty())
    }

    @Test
    fun `detects snake_case variable name with default camelCase config`() {
        val code =
            """
            let user_name: string = "Alice";
            println(user_name);
            """.trimIndent()

        val program = LinterPsSupport.parse(code)
        val linter = DefaultLinterFactory.create(defaultConfig)
        val report = linter.lint(program)

        assertFalse(report.isOk)
        assertEquals(1, report.errors.size)

        val error = assertIs<InvalidIdentifierFormat>(report.errors.first())
        assertEquals("user_name", error.identifier)
        assertEquals("camelCase", error.expectedFormat)
//        assertEquals(1, error.location.start.line)
    }

    @Test
    fun `detects println called with binary expression`() {
        val code =
            """
            let total: number = 10;
            println(total + 5);
            """.trimIndent()

        val program = LinterPsSupport.parse(code)
        val linter = DefaultLinterFactory.create(defaultConfig)
        val report = linter.lint(program)

        assertFalse(report.isOk)
        assertEquals(1, report.errors.size)

        val error = assertIs<InvalidPrintlnArgument>(report.errors.first())
//        assertEquals(2, error.location.start.line)
    }

    @Test
    fun `accumulates multiple violations in source order`() {
        val code =
            """
            let first_name: string = "Bob";
            let last_name: string = "Smith";
            println(first_name + " " + last_name);
            """.trimIndent()

        val program = LinterPsSupport.parse(code)
        val linter = DefaultLinterFactory.create(defaultConfig)
        val report = linter.lint(program)

        assertFalse(report.isOk)
        assertEquals(3, report.errors.size)

        val error1 = assertIs<InvalidIdentifierFormat>(report.errors[0])
        assertEquals("first_name", error1.identifier)
//        assertEquals(1, error1.location.start.line)

        val error2 = assertIs<InvalidIdentifierFormat>(report.errors[1])
        assertEquals("last_name", error2.identifier)
//        assertEquals(2, error2.location.start.line)

        val error3 = assertIs<InvalidPrintlnArgument>(report.errors[2])
//        assertEquals(3, error3.location.start.line)
    }

    @Test
    fun `supports snake_case identifier format configuration`() {
        val config =
            LinterConfig(
                rules =
                    mapOf(
                        "identifier-format" to RuleConfig(enabled = true, options = mapOf("format" to "snake_case")),
                        "println-simple-argument" to RuleConfig(enabled = true),
                    ),
            )

        val code =
            """
            let valid_name: string = "ok";
            let invalidCamel: number = 1;
            println(valid_name);
            """.trimIndent()

        val program = LinterPsSupport.parse(code)
        val linter = DefaultLinterFactory.create(config)
        val report = linter.lint(program)

        assertFalse(report.isOk)
        assertEquals(1, report.errors.size)

        val error = assertIs<InvalidIdentifierFormat>(report.errors.first())
        assertEquals("invalidCamel", error.identifier)
        assertEquals("snake_case", error.expectedFormat)
//        assertEquals(2, error.location.start.line)
    }
}
