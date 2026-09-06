package printscript.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrintScriptCliTest {
    @Test
    fun `run prints handler output and exits 0`() {
        val result =
            cli(
                run = FileCommand { CommandResult.Output("hello\n") },
            ).capture(arrayOf("run", "foo.ps"))

        assertEquals(0, result.statusCode)
        assertEquals("hello\n", result.stdout)
    }

    @Test
    fun `lint prints OK when the handler succeeds`() {
        val result = cli().capture(arrayOf("lint", "foo.ps"))

        assertEquals(0, result.statusCode)
        assertEquals("OK\n", result.stdout)
    }

    @Test
    fun `typecheck prints failures to stderr and exits 1`() {
        val result =
            cli(
                typecheck = FileCommand { CommandResult.Failed(listOf("Se esperaba number (1:5-1:10)")) },
            ).capture(arrayOf("typecheck", "foo.ps"))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("Se esperaba number (1:5-1:10)"))
    }

    @Test
    fun `format prints formatted source without OK`() {
        val result =
            cli(
                format = FileCommand { CommandResult.Output("1 + 2;\n") },
            ).capture(arrayOf("format", "foo.ps"))

        assertEquals(0, result.statusCode)
        assertEquals("1 + 2;\n", result.stdout)
    }

    @Test
    fun `unsupported language version prints ERROR`() {
        val result = cli().capture(arrayOf("--version", "2.0", "run", "foo.ps"))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("ERROR"))
    }

    @Test
    fun `missing subcommand prints help`() {
        val result = cli().capture(arrayOf())

        assertTrue(result.stdout.contains("Usage") || result.stderr.contains("Usage"))
    }

    @Test
    fun `extra file command is registered without changing the root`() {
        val result =
            PrintScriptCli(
                FileCliCommand("analyze", "Analyze a PrintScript file") { CommandResult.Ok },
            ).capture(arrayOf("analyze", "foo.ps"))

        assertEquals(0, result.statusCode)
        assertEquals("OK\n", result.stdout)
    }

    private fun cli(
        run: FileCommand = FileCommand { CommandResult.Ok },
        lint: FileCommand = FileCommand { CommandResult.Ok },
        check: FileCommand = FileCommand { CommandResult.Ok },
        format: FileCommand = FileCommand { CommandResult.Ok },
        typecheck: FileCommand = FileCommand { CommandResult.Ok },
    ) = PrintScriptCli(
        FileCliCommand("run", "Execute a PrintScript file", printOk = false, run),
        FileCliCommand("lint", "Lint a PrintScript file", command = lint),
        FileCliCommand("check", "Check that a PrintScript file matches the formatter", command = check),
        FileCliCommand("format", "Format a PrintScript file and print it to stdout", printOk = false, format),
        FileCliCommand("typecheck", "Type-check a PrintScript file", command = typecheck),
    )
}
