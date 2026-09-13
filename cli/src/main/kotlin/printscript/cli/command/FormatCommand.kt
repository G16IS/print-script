package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.emit
import printscript.cli.presentFormat
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.formatter.Formatter
import printscript.reader.FileCodeReader
import printscript.usecases.FormatCode

class FormatCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val formatter: Formatter,
) : SourceFileCommand("format", "Format a PrintScript file and print it to stdout") {
    override fun run() {
        emit(
            presentFormat {
                FormatCode.formatCode(lang, grammar, FileCodeReader(file), formatter)
            },
            printOk = false,
        )
    }
}
