package printscript.infrastructure.cli

import com.github.ajalt.clikt.core.CliktCommand
import printscript.cli.command.CheckCommand
import printscript.cli.command.FormatCommand
import printscript.cli.command.LintCommand
import printscript.cli.command.RunCommand
import printscript.cli.command.TypeCheckCommand

fun interface CommandFactory {
    fun create(configs: PrintScriptConfigs): CliktCommand
}

object RunCommandFactory : CommandFactory {
    override fun create(configs: PrintScriptConfigs) =
        RunCommand(
            lang = configs.lang,
            grammar = configs.grammar,
            typeSystem = configs.typeSystem,
            sources = FileSources,
            effects = RunEffects,
        )
}

object LintCommandFactory : CommandFactory {
    override fun create(configs: PrintScriptConfigs) =
        LintCommand(
            lang = configs.lang,
            grammar = configs.grammar,
            linterConfig = configs.linterConfig,
            sources = FileSources,
            effects = LintEffects,
        )
}

object CheckCommandFactory : CommandFactory {
    override fun create(configs: PrintScriptConfigs) =
        CheckCommand(
            lang = configs.lang,
            grammar = configs.grammar,
            formatter = configs.formatter,
            sources = FileSources,
            effects = CheckEffects,
        )
}

object FormatCommandFactory : CommandFactory {
    override fun create(configs: PrintScriptConfigs) =
        FormatCommand(
            lang = configs.lang,
            grammar = configs.grammar,
            formatter = configs.formatter,
            sources = FileSources,
            effects = FormatEffects,
        )
}

object TypeCheckCommandFactory : CommandFactory {
    override fun create(configs: PrintScriptConfigs) =
        TypeCheckCommand(
            lang = configs.lang,
            grammar = configs.grammar,
            typeSystem = configs.typeSystem,
            sources = FileSources,
            effects = TypeCheckEffects,
        )
}

object CommandFactories {
    fun defaults(): List<CommandFactory> =
        listOf(
            RunCommandFactory,
            LintCommandFactory,
            CheckCommandFactory,
            FormatCommandFactory,
            TypeCheckCommandFactory,
        )
}
