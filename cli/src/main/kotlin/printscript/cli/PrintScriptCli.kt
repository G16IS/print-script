package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
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
import printscript.util.unwrap

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
        if (LanguageCatalog.of(version, DefaultSideEffectManager())
                is Result.Err
        ) {
            echo("ERROR", err = true)
            throw ProgramResult(1)
        }
    }

    companion object {
        fun create(version: String = DEFAULT_VERSION): PrintScriptCli {
            val configs = PrintScriptConfigsLoader.load("1.0")
            val sideEffectManager = DefaultSideEffectManager()

            val langKit =
                LanguageCatalog
                    .of(version, sideEffectManager)
                    .unwrap("Failed to load language kit for version $version")

            return create(configs, langKit)
        }

        fun create(
            configs: PrintScriptConfigs,
            langKit: LanguageKit,
        ): PrintScriptCli =
            PrintScriptCli(
                RunCommand(configs.lang, configs.grammar, configs.typeSystem, langKit),
                LintCommand(configs.lang, configs.grammar, configs.linterConfig, langKit),
                CheckCommand(configs.lang, configs.grammar, configs.formatter, langKit),
                FormatCommand(configs.lang, configs.grammar, configs.formatter, langKit),
                TypeCheckCommand(configs.lang, configs.grammar, configs.typeSystem, langKit),
            )

        private const val DEFAULT_VERSION = "1.0"
    }
}
