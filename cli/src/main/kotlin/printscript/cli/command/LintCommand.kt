package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.emit
import printscript.cli.formatLintError
import printscript.cli.presentReport
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.infrastructure.reader.FileCodeReader
import usecases.LintProgram

class LintCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val linterConfig: LinterConfig,
) : SourceFileCommand("lint", "Lint a PrintScript file") {
    override fun run() {
        emit(
            presentReport(
                { LintProgram.lint(lang, grammar, FileCodeReader(file), linterConfig) },
                ::formatLintError,
            ),
        )
    }
}
