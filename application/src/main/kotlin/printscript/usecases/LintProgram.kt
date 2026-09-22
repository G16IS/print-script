package printscript.usecases

import printscript.config.PrintScriptConfigs
import printscript.definitions.ErrorHandler
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.error.formatError
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
        kit: LanguageKit,
    ): Report<SyntaxProgram, Error> =
        when (val program = ParseProgram.parse(langConfig, grammar, reader, kit)) {
            is Result.Ok -> {
                val linted = DefaultLinterFactory.create(linterConfig).lint(program.value)
                Report(value = linted.value, errors = linted.errors)
            }

            is Result.Err -> program.toReport()
        }

    /**
     * Entrypoint del TCK: cada violación (de lint o de parseo) va al [errorHandler] con su
     * ubicación, y el recorrido no se corta ante la primera.
     */
    fun lintForTck(
        configs: PrintScriptConfigs,
        codeReader: CodeReader,
        errorHandler: ErrorHandler,
        languageKit: LanguageKit,
    ) {
        lint(configs.lang, configs.grammar, codeReader, configs.linterConfig, languageKit)
            .errors
            .forEach { errorHandler.handleErrorMessage(formatError(it)) }
    }
}
