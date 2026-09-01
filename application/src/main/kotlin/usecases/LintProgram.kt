package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.error.LintError
import printscript.factory.DefaultLinterFactory
import printscript.syntax.SyntaxProgram
import printscript.util.Report

internal object LintProgram {
    fun lint(
        langConfig: LanguageConfig,
        grammar: Grammar,
        path: String,
        linterConfig: LinterConfig,
    ): Report<SyntaxProgram, LintError> {
        val program: SyntaxProgram = ParseProgram.parse(langConfig, grammar, path)

        val linter = DefaultLinterFactory.create(linterConfig)

        val lintReport: Report<SyntaxProgram, LintError> = linter.lint(program)

        return lintReport
    }
}
