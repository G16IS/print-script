package printscript.cli

import com.github.ajalt.clikt.testing.test
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

        val result = cli("run", file)

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

        val result = cli("typecheck", file)

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("Se esperaba number"))
    }

    @Test
    fun `format pretty-prints an expression`() {
        val file = sourceFile("1+2;")

        val result = cli("format", file)

        assertEquals(0, result.statusCode)
        assertEquals("1 + 2;\n", result.stdout)
    }

    @Test
    fun `check fails on unformatted source`() {
        val file = sourceFile("1+2;")

        val result = cli("check", file)

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.isNotBlank())
    }

    @Test
    fun `parse failure prints ERROR`() {
        val file = sourceFile("let")

        val result = cli("run", file)

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("Expected ID, found EOF (1:4-1:4)"))
    }

    @Test
    fun `unsupported language version prints ERROR`() {
        val file = sourceFile("1+2;")

        val result = cli("--version", "2.0", "run", file)

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("ERROR"))
    }

    @Test
    fun `missing subcommand prints help`() {
        val result = cli()

        assertTrue(result.stdout.contains("Usage") || result.stderr.contains("Usage"))
    }

    private fun cli(vararg args: String) = PrintScriptCli.create().test(*args)

    private fun sourceFile(contents: String): String {
        val file = tempDir.resolve("sample.ps")
        Files.writeString(file, contents)
        return file.toAbsolutePath().toString()
    }
}
