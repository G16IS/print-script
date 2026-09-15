package printscript.cli.command

import java.nio.file.Files
import java.nio.file.Path
import printscript.cli.SourceFileCommand
import printscript.cli.emit
import printscript.cli.presentReport
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.edition.LanguageKit
import printscript.error.formatError
import printscript.formatter.Formatter
import printscript.reader.FileCodeReader
import printscript.usecases.CheckFormat

class CheckCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val formatter: Formatter,
    private val languageKit: LanguageKit,
) : SourceFileCommand("check", "Check that a PrintScript file matches the formatter") {
    override fun run() {
        emit(
            presentReport(
                {
                    CheckFormat.checkFormat(
                        lang,
                        grammar,
                        FileCodeReader(file),
                        Files.readString(Path.of(file)),
                        formatter,
                        languageKit,
                    )
                },
                ::formatError,
            ),
        )
    }
}
