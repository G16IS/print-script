package printscript.cli.command

import printscript.cli.CommandEffects
import printscript.cli.SourceFileCommand
import printscript.cli.SourceFiles
import printscript.cli.emit
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.error.LintError
import printscript.syntax.SyntaxProgram
import printscript.util.Report
import usecases.LintProgram

class LintCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val linterConfig: LinterConfig,
    private val sources: SourceFiles,
    private val effects: CommandEffects<Report<SyntaxProgram, LintError>>,
) : SourceFileCommand("lint", "Lint a PrintScript file") {
    override fun run() {
        emit(
            effects.handle {
                LintProgram.lint(lang, grammar, sources.reader(file), linterConfig)
            },
        )
    }
}
