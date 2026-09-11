package edu.austral.dissis.usecases

import edu.austral.dissis.testing.PrintScriptLanguage
import java.io.File
import java.io.InputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.PrintEffect
import printscript.domain.Grammar
import printscript.domain.TypeSystemConfig
import printscript.infrastructure.reader.FileCodeReader
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.infrastructure.reader.JSONTypeSystemConfigReader
import printscript.util.Result
import usecases.ExecuteCode
import usecases.ExecutionFailure

class ExecuteCodeTest {
    private val language = PrintScriptLanguage.config()
    private val grammar: Grammar = JSONGrammarConfigReader.read(stream("grammar.config.v1.json"))
    private val typeSystem: TypeSystemConfig =
        JSONTypeSystemConfigReader.read(stream("type-system.config.v1.json"))

    @Test
    fun `declarations and prints emit printable side effects`() {
        val result = execute("declarations_and_prints.ps")

        assertTrue(result is Result.Ok)
        assertEquals(
            listOf("Hello, World!", "42"),
            (result as Result.Ok).value.map { (it as PrintEffect).text },
        )
    }

    @Test
    fun `binary expression prints the evaluated result`() {
        val result = execute("binary_expression.ps")

        assertTrue(result is Result.Ok)
        assertEquals(listOf("7"), (result as Result.Ok).value.map { (it as PrintEffect).text })
    }

    @Test
    fun `type mismatch does not run the interpreter`() {
        val result = execute("type_mismatch.ps")

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is ExecutionFailure.Types)
    }

    private fun execute(example: String) =
        ExecuteCode.execute(
            language,
            grammar,
            typeSystem,
            FileCodeReader(file("examples/$example")),
        )

    private fun stream(name: String): InputStream =
        requireNotNull(loader().getResourceAsStream(name)) { "Missing resource $name" }

    private fun file(name: String): String {
        val url = requireNotNull(loader().getResource(name)) { "Missing resource $name" }
        return File(url.toURI()).absolutePath
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader
}
