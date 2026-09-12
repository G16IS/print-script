package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import printscript.application.config.PrintScriptConfigs
import printscript.cli.command.CheckCommand
import printscript.cli.command.FormatCommand
import printscript.cli.command.LintCommand
import printscript.cli.command.RunCommand
import printscript.cli.command.TypeCheckCommand
import printscript.tck.PrintScriptConfigsLoader

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

    companion object {
        fun create(): PrintScriptCli {
            val configs = PrintScriptConfigsLoader.load("1")
            return PrintScriptCli(
                RunCommand(configs.lang, configs.grammar, configs.typeSystem),
                LintCommand(configs.lang, configs.grammar, configs.linterConfig),
                CheckCommand(configs.lang, configs.grammar, configs.formatter),
                FormatCommand(configs.lang, configs.grammar, configs.formatter),
                TypeCheckCommand(configs.lang, configs.grammar, configs.typeSystem),
            )
        }

        fun create(configs: PrintScriptConfigs): PrintScriptCli =
            PrintScriptCli(
                RunCommand(configs.lang, configs.grammar, configs.typeSystem),
                LintCommand(configs.lang, configs.grammar, configs.linterConfig),
                CheckCommand(configs.lang, configs.grammar, configs.formatter),
                FormatCommand(configs.lang, configs.grammar, configs.formatter),
                TypeCheckCommand(configs.lang, configs.grammar, configs.typeSystem),
            )

        private const val DEFAULT_VERSION = "1.0"
    }
}
