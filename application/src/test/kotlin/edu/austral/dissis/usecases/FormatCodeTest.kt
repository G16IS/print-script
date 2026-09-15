package edu.austral.dissis.usecases

import edu.austral.dissis.testing.FormatExample
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import printscript.edition.LanguageCatalog
import printscript.io.DefaultSideEffectManager
import printscript.reader.FileCodeReader
import printscript.tck.PrintScriptConfigsLoader
import printscript.usecases.FormatCode
import printscript.util.Result

class FormatCodeTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `formats an unformatted expression file`() {
        val formatted = FormatExample.format("unformatted_expression.ps")

        assertTrue(formatted is Result.Ok)
        assertEquals("1 + 2;\n", (formatted as Result.Ok).value)
    }

    @Test
    fun `formats an unformatted declaration file`() {
        val formatted = FormatExample.format("unformatted_declaration.ps")

        assertTrue(formatted is Result.Ok)
        assertEquals("let x : number = 1;\n", (formatted as Result.Ok).value)
    }

    @Test
    fun `formatForTck throws when format fails`() {
        val configs = PrintScriptConfigsLoader.load("1.0")
        val kit =
            (LanguageCatalog.of("1.0", DefaultSideEffectManager()) as Result.Ok).value
        val source = tempDir.resolve("bad.ps")
        Files.writeString(source, "let")
        val writer = StringWriter()

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                FormatCode.formatForTck(
                    configs,
                    FileCodeReader(source.toAbsolutePath().toString()),
                    writer,
                    kit,
                )
            }

        assertTrue(thrown.message!!.startsWith("TCK format failed:"))
        assertEquals("", writer.toString())
    }

    @Test
    fun `formats an uninitialized declaration without assign`() {
        val formatted = FormatExample.format("unformatted_uninitialized.ps")

        assertTrue(formatted is Result.Ok)
        assertEquals("let x : string;\n", (formatted as Result.Ok).value)
    }
}
