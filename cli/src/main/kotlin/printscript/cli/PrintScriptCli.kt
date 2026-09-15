package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import printscript.cli.command.CheckCommand
import printscript.cli.command.FormatCommand
import printscript.cli.command.LintCommand
import printscript.cli.command.RunCommand
import printscript.cli.command.TypeCheckCommand
import printscript.config.PrintScriptConfigs
import printscript.edition.LanguageCatalog
import printscript.edition.LanguageKit
import printscript.io.DefaultSideEffectManager
import printscript.tck.PrintScriptConfigsLoader
import printscript.util.Result

class PrintScriptCli : CliktCommand(name = "printscript") {
    private val version by option(
        "--version",
        help = "PrintScript language version",
    )

    override val printHelpOnEmptyArgs: Boolean = true

    override fun help(context: Context): String = "PrintScript command line interface"

    override fun run() {
        val ver =
            version ?: run {
                echo("ERROR", err = true)
                throw ProgramResult(1)
            }

        val kit =
            loadKit(ver) ?: run {
                echo("ERROR", err = true)
                throw ProgramResult(1)
            }

        currentContext.findOrSetObject { kit }
    }

    companion object {
        fun create(): PrintScriptCli =
            PrintScriptCli().subcommands(
                RunCommand(),
                LintCommand(),
                CheckCommand(),
                FormatCommand(),
                TypeCheckCommand(),
            )

        private fun loadKit(version: String): VersionKit? {
            val sideEffectManager = DefaultSideEffectManager()
            val langKit = LanguageCatalog.of(version, sideEffectManager)
            if (langKit is Result.Err) return null
            val configs = PrintScriptConfigsLoader.load(version)
            return VersionKit(configs, (langKit as Result.Ok).value)
        }
    }
}

data class VersionKit(
    val configs: PrintScriptConfigs,
    val langKit: LanguageKit,
)
