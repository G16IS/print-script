package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.emit
import printscript.cli.formatError
import printscript.cli.presentReport
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.edition.LanguageKit
import printscript.reader.FileCodeReader
import printscript.usecases.LintProgram

class LintCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val linterConfig: LinterConfig,
    private val languageKit: LanguageKit,
) : SourceFileCommand("lint", "Lint a PrintScript file") {
    override fun run() {
        emit(
            presentReport(
                { LintProgram.lint(lang, grammar, FileCodeReader(file), linterConfig, languageKit) },
                ::formatError,
            ),
        )
    }
}
