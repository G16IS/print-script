package printscript.tck

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.definitions.ErrorHandler

/**
 * Replica los casos de `printscript-tck/src/test/resources/linter/1.0/`.
 *
 * El TCK no compara texto: asserta lista de errores vacía para los `valid-*` y no vacía
 * para los `invalid-*`. Los config JSON de acá son los que produciría el
 * `TckLinterConfigAdapter` a partir de la config plana del TCK.
 */
class PrintScriptLintTckTest {
    @Test
    fun `valid-no-rules reports nothing even though an identifier is not camelCase`() {
        val errors =
            lintWith(
                config = "{}",
                source = "let myVariable: number = 5;\nlet my_variable: number = 10;",
                version = "1.0",
            )

        // Si la config del TCK se mergeara con linter.config.v1.0.json (que trae
        // identifier-format enabled), `my_variable` dispararía y este caso rompería.
        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `valid-mandatory-camel-case-identifiers`() {
        val errors = lintWith(identifierFormat("camelCase"), "let myVariable: number = 5;", "1.0")

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `invalid-mandatory-camel-case-identifiers`() {
        val errors = lintWith(identifierFormat("snake_case"), "let myVariable: number = 5;", "1.0")

        assertTrue(errors.isNotEmpty(), "esperaba al menos un error, hubo $errors")
    }

    @Test
    fun `valid-mandatory-snake-case-identifiers`() {
        val errors = lintWith(identifierFormat("snake_case"), "let my_variable: number = 5;", "1.0")

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `invalid-mandatory-snake-case-identifiers`() {
        val errors = lintWith(identifierFormat("camelCase"), "let my_variable: number = 5;", "1.0")

        assertTrue(errors.isNotEmpty(), "esperaba al menos un error, hubo $errors")
    }

    @Test
    fun `invalid-println-with-expression`() {
        val config =
            """
            { "rules": { "println-simple-argument": { "enabled": true, "options": { "callee": "println" } } } }
            """.trimIndent()
        val errors = lintWith(config, """println("Hello" + "World!");""", "1.0")

        assertTrue(errors.isNotEmpty(), "esperaba al menos un error, hubo $errors")
    }

    @Test
    fun `a disabled rule reports nothing`() {
        val config =
            """
            { "rules": { "identifier-format": { "enabled": false, "options": { "format": "camelCase" } } } }
            """.trimIndent()
        val errors = lintWith(config, "let my_variable: number = 5;", "1.0")

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `each message carries start and end position`() {
        val errors = lintWith(identifierFormat("camelCase"), "let my_variable: number = 5;", "1.0")

        assertEquals(1, errors.size)
        assertTrue(
            Regex("""^.+ \(\d+:\d+-\d+:\d+\)$""").matches(errors.single()),
            "esperaba 'mensaje (l:c-l:c)' pero fue: ${errors.single()}",
        )
    }

    @Test
    fun `a syntax error also reaches the error handler`() {
        val errors = lintWith("{}", "let x number = ;", "1.0")

        assertTrue(errors.isNotEmpty(), "esperaba que el error de parseo se reportara")
    }

    @Test
    fun `an unknown version is reported instead of throwing`() {
        val collector = CollectingErrorHandler()

        PrintScript.lint(
            "9.9",
            StringCodeReader("let x: number = 1;"),
            ByteArrayInputStream("{}".toByteArray(StandardCharsets.UTF_8)),
            collector,
        )

        assertEquals(listOf("version 9.9 not found"), collector.errors)
    }

    private fun identifierFormat(format: String): String =
        """
        { "rules": { "identifier-format": { "enabled": true, "options": { "format": "$format" } } } }
        """.trimIndent()

    private fun lintWith(
        config: String,
        source: String,
        version: String,
    ): List<String> {
        val collector = CollectingErrorHandler()

        PrintScript.lint(
            version,
            StringCodeReader(source),
            ByteArrayInputStream(config.toByteArray(StandardCharsets.UTF_8)),
            collector,
        )

        return collector.errors
    }

    /** Equivalente al `ErrorCollector` del TCK. */
    private class CollectingErrorHandler : ErrorHandler {
        val errors = mutableListOf<String>()

        override fun handleErrorMessage(errorMessage: String) {
            errors += errorMessage
        }
    }
}
