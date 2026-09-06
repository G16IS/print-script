package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.testing.test

class PrintScriptCli(
    vararg commands: CliktCommand,
) : CliktCommand(name = "printscript") {
    private val version by option(
        "--version",
        help = "PrintScript language version",
    ).default(DEFAULT_VERSION)

    init {
        subcommands(*commands)
    }

    override val printHelpOnEmptyArgs: Boolean = true

    override fun help(context: Context): String = "PrintScript command line interface"

    override fun run() {
        if (version != DEFAULT_VERSION) {
            echo("ERROR", err = true)
            throw ProgramResult(1)
        }
    }

    fun runCli(args: Array<String>) {
        main(args)
    }

    fun capture(args: Array<String>): CliExecution {
        val result = test(*args)
        return CliExecution(
            statusCode = result.statusCode,
            stdout = result.stdout,
            stderr = result.stderr,
        )
    }

    private companion object {
        const val DEFAULT_VERSION = "1.0"
    }
}
