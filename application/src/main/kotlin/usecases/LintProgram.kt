package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.error.LintError
import printscript.factory.DefaultLinterFactory
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.util.Report

object LintProgram {
    fun lint(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        linterConfig: LinterConfig,
        onStatement: () -> Unit = {},
    ): Report<SyntaxProgram, LintError> {
        val program = ParseProgram.parse(langConfig, grammar, reader, onStatement)

        return DefaultLinterFactory.create(linterConfig).lint(program)
    }
}
