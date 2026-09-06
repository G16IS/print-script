package printscript.cli.command

import printscript.cli.CommandEffects
import printscript.cli.SourceFileCommand
import printscript.cli.SourceFiles
import printscript.cli.emit
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.FormatError
import printscript.formatter.Formatter
import printscript.util.Report
import usecases.CheckFormat

class CheckCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val formatter: Formatter,
    private val sources: SourceFiles,
    private val effects: CommandEffects<Report<Unit, FormatError>>,
) : SourceFileCommand("check", "Check that a PrintScript file matches the formatter") {
    override fun run() {
        emit(
            effects.handle {
                CheckFormat.checkFormat(
                    lang,
                    grammar,
                    sources.reader(file),
                    sources.text(file),
                    formatter,
                )
            },
        )
    }
}
