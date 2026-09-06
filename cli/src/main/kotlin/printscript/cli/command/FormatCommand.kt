package printscript.cli.command

import printscript.cli.CommandEffects
import printscript.cli.SourceFileCommand
import printscript.cli.SourceFiles
import printscript.cli.emit
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.FormatError
import printscript.formatter.Formatter
import printscript.util.Result
import usecases.FormatCode

class FormatCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val formatter: Formatter,
    private val sources: SourceFiles,
    private val effects: CommandEffects<Result<String, FormatError>>,
) : SourceFileCommand("format", "Format a PrintScript file and print it to stdout", printOk = false) {
    override fun run() {
        emit(
            effects.handle {
                FormatCode.formatCode(lang, grammar, sources.reader(file), formatter)
            },
            printOk = false,
        )
    }
}
