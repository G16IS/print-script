package printscript.usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.error.Error
import printscript.factory.DefaultLinterFactory
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.util.Report
import printscript.util.Result
import printscript.util.toReport

object LintProgram {
    fun lint(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        linterConfig: LinterConfig,
    ): Report<SyntaxProgram, Error> =
        when (val program = ParseProgram.parse(langConfig, grammar, reader)) {
            is Result.Ok -> {
                val linted = DefaultLinterFactory.create(linterConfig).lint(program.value)
                Report(value = linted.value, errors = linted.errors)
            }
            is Result.Err -> program.toReport()
        }
}
