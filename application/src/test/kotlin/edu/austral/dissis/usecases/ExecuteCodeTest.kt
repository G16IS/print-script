package edu.austral.dissis.usecases

import edu.austral.dissis.testing.PrintScriptLanguage
import java.io.File
import java.io.InputStream
import java.util.Optional
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
import printscript.reader.CharPosition
import printscript.reader.CodeReader
import printscript.reader.FileCodeReader
import printscript.reader.JSONGrammarConfigReader
import printscript.reader.JSONTypeSystemConfigReader
import printscript.usecases.ExecuteCode
import printscript.usecases.TypecheckCode
import printscript.util.Result
import printscript.util.isOk

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
    fun `type error stops execution before later statements`() {
        val seen = RecordingSideEffects()
        val code = "println(\"before\");\nlet x: number = \"hola\";\nprintln(\"after\");"
        val result =
            ExecuteCode.execute(
                language,
                grammar,
                typeSystem,
                StringCodeReader(code),
                LanguageCatalog.v10(seen),
            )

        assertFalse(result.isOk)
        assertTrue((result as Result.Err).error is TypeErrorWithMessage)
        assertEquals(listOf("before"), seen.printed())
    }

    @Test
    fun `type mismatch does not run the interpreter`() {
        val result = execute("type_mismatch.ps")

        assertFalse(result.isOk)
        assertTrue((result as Result.Err).error is TypeErrorWithMessage)
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

    @Test
    fun `executes statements in a streaming fashion without accumulating the entire tree`() {
        val seen = RecordingSideEffects()
        val totalStatements = 1000
        val code = (1..totalStatements).joinToString("\n") { "println($it);" }
        val result =
            ExecuteCode.execute(
                language,
                grammar,
                typeSystem,
                StringCodeReader(code),
                LanguageCatalog.v10(seen),
            )
        assertTrue(result.isOk)
        assertEquals(totalStatements, seen.printed().size)
        assertEquals("1", seen.printed().first())
        assertEquals(totalStatements.toString(), seen.printed().last())
    }

    @Test
    fun `streaming execution executes statements up to a runtime error`() {
        val seen = RecordingSideEffects()
        val code = "println(\"first\");\nprintln(undeclaredVar);"
        val result =
            ExecuteCode.execute(
                language,
                grammar,
                typeSystem,
                StringCodeReader(code),
                LanguageCatalog.v10(seen),
            )
        assertFalse(result.isOk)
        assertEquals(listOf("first"), seen.printed())
    }

    @Test
    fun `check validates types without building an AST`() {
        val code = "let x: number = 10;\nprintln(x);"
        val result =
            TypecheckCode.check(
                language,
                grammar,
                typeSystem,
                StringCodeReader(code),
                LanguageCatalog.v10(RecordingSideEffects()),
            )
        assertTrue(result.isOk)
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

    private class StringCodeReader(
        source: String,
    ) : CodeReader {
        private val realReader = source.reader().buffered()
        private var currentPosition = CharPosition(1, 1)
        private var lookahead = realReader.read()

        override fun read(): Optional<Char> {
            if (lookahead == -1) return Optional.empty()

            val char = lookahead.toChar()
            currentPosition =
                if (char == '\n') {
                    CharPosition(currentPosition.line + 1, 1)
                } else {
                    CharPosition(currentPosition.line, currentPosition.col + 1)
                }

            lookahead = realReader.read()
            return Optional.of(char)
        }

        override fun peek(): Optional<Char> {
            if (lookahead == -1) return Optional.empty()
            return Optional.of(lookahead.toChar())
        }

        override fun currentPosition(): CharPosition = currentPosition
    }

    private fun stream(name: String): InputStream =
        requireNotNull(loader().getResourceAsStream(name)) { "Missing resource $name" }

    private fun file(name: String): String {
        val url = requireNotNull(loader().getResource(name)) { "Missing resource $name" }
        return File(url.toURI()).absolutePath
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader
}
