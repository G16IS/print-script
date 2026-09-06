package printscript.cli

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PrintScriptCliTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `run prints println output`() {
        val file =
            sourceFile(
                """
                let pepe: string = "Hello, World!";
                println(pepe);
                """.trimIndent(),
            )

        val result = PrintScriptCli.create().capture(arrayOf("run", file))

        assertEquals(0, result.statusCode)
        assertEquals("Hello, World!\n", result.stdout)
    }

    @Test
    fun `typecheck reports a type error`() {
        val file =
            sourceFile(
                """
                let x: number = "hola";
                """.trimIndent(),
            )

        val result = PrintScriptCli.create().capture(arrayOf("typecheck", file))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("Se esperaba number"))
    }

    @Test
    fun `format pretty-prints an expression`() {
        val file = sourceFile("1+2;")

        val result = PrintScriptCli.create().capture(arrayOf("format", file))

        assertEquals(0, result.statusCode)
        assertEquals("1 + 2;\n", result.stdout)
    }

    @Test
    fun `check fails on unformatted source`() {
        val file = sourceFile("1+2;")

        val result = PrintScriptCli.create().capture(arrayOf("check", file))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.isNotBlank())
    }

    @Test
    fun `parse failure prints ERROR`() {
        val file = sourceFile("let")

        val result = PrintScriptCli.create().capture(arrayOf("run", file))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("ERROR"))
    }

    @Test
    fun `unsupported language version prints ERROR`() {
        val file = sourceFile("1+2;")

        val result = PrintScriptCli.create().capture(arrayOf("--version", "2.0", "run", file))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("ERROR"))
    }

    @Test
    fun `missing subcommand prints help`() {
        val result = PrintScriptCli.create().capture(arrayOf())

        assertTrue(result.stdout.contains("Usage") || result.stderr.contains("Usage"))
    }

    private fun sourceFile(contents: String): String {
        val file = tempDir.resolve("sample.ps")
        Files.writeString(file, contents)
        return file.toAbsolutePath().toString()
    }
}
