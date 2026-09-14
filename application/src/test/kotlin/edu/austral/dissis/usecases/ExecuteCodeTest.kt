package edu.austral.dissis.usecases

import edu.austral.dissis.testing.PrintScriptLanguage
import java.io.File
import java.io.InputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.PrintEffect
import printscript.SideEffect
import printscript.SideEffectManager
import printscript.domain.Grammar
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageCatalog
import printscript.error.TypeErrorWithMessage
import printscript.reader.FileCodeReader
import printscript.reader.JSONGrammarConfigReader
import printscript.reader.JSONTypeSystemConfigReader
import printscript.usecases.ExecuteCode
import printscript.util.Result

class ExecuteCodeTest {
    private val language = PrintScriptLanguage.config()
    private val grammar: Grammar = JSONGrammarConfigReader.read(stream("grammar.config.v1.0.json"))
    private val typeSystem: TypeSystemConfig =
        JSONTypeSystemConfigReader.read(stream("type-system.config.v1.0.json"))

    @Test
    fun `declarations and prints emit printable side effects`() {
        val seen = RecordingSideEffects()
        val result = execute("declarations_and_prints.ps", seen)

        assertTrue(result.isOk)
        assertEquals(listOf("Hello, World!", "42"), seen.printed())
    }

    @Test
    fun `binary expression prints the evaluated result`() {
        val seen = RecordingSideEffects()
        val result = execute("binary_expression.ps", seen)

        assertTrue(result.isOk)
        assertEquals(listOf("7"), seen.printed())
    }

    @Test
    fun `type mismatch does not run the interpreter`() {
        val result = execute("type_mismatch.ps")

        assertFalse(result.isOk)
        assertTrue(result.errors.first() is TypeErrorWithMessage)
    }

    @Test
    fun `execute with v11 kit still runs v1 programs`() {
        val seen = RecordingSideEffects()
        val kit =
            LanguageCatalog
                .of("1.1", seen)
                .let { (it as Result.Ok).value }
        val result =
            ExecuteCode.execute(
                language,
                grammar,
                typeSystem,
                FileCodeReader(file("examples/binary_expression.ps")),
                kit,
            )
        assertTrue(result.isOk)
        assertEquals(listOf("7"), seen.printed())
    }

    private fun execute(
        example: String,
        sideEffects: SideEffectManager = RecordingSideEffects(),
    ) = ExecuteCode.execute(
        language,
        grammar,
        typeSystem,
        FileCodeReader(file("examples/$example")),
        LanguageCatalog.v10(sideEffects),
    )

    private class RecordingSideEffects : SideEffectManager {
        val effects = mutableListOf<SideEffect>()

        override fun handle(effect: SideEffect): String? {
            effects += effect
            return null
        }

        fun printed(): List<String> = effects.map { (it as PrintEffect).text }
    }

    private fun stream(name: String): InputStream =
        requireNotNull(loader().getResourceAsStream(name)) { "Missing resource $name" }

    private fun file(name: String): String {
        val url = requireNotNull(loader().getResource(name)) { "Missing resource $name" }
        return File(url.toURI()).absolutePath
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader
}
