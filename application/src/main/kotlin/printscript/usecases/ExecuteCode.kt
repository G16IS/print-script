package printscript.usecases

import printscript.DefaultInterpreterFactory
import printscript.ErrorHandler
import printscript.InterpreterContext
import printscript.config.PrintScriptConfigs
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.reader.CodeReader
import printscript.util.Report
import printscript.util.Result
import printscript.util.fold
import printscript.util.isOk

object ExecuteCode {
    fun execute(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        kit: LanguageKit,
    ): Report<Unit, Error> {
        val report = TypecheckCode.typecheck(langConfig, grammar, typeSystem, reader, kit)

        if (!report.isOk) {
            return Report(errors = report.errors)
        }

        return DefaultInterpreterFactory
            .create(kit.evaluators, kit.executors)
            .interpret(InterpreterContext(), report.value!!)
            .fold(
                onOk = { Report(value = Unit) },
                onErr = { Report(errors = listOf(it)) },
            )
    }

    fun executeForTck(
        configs: PrintScriptConfigs,
        codeReader: CodeReader,
        errorHandler: ErrorHandler,
        languageKit: LanguageKit,
    ) {
        val report = TypecheckCode.typecheck(configs.lang, configs.grammar, configs.typeSystem, codeReader, languageKit)

        if (!report.isOk) {
            report.errors.forEach { reportError(it, errorHandler) }
        }

        val evaluators = languageKit.evaluators
        val statementExecutors = languageKit.executors

        val interpreterResult =
            DefaultInterpreterFactory
                .create(evaluators, statementExecutors)
                .interpret(InterpreterContext(), report.value!!)

        if (interpreterResult is Result.Err) reportError((interpreterResult).error, errorHandler)
    }
}

private fun reportError(
    error: Error,
    errorHandler: ErrorHandler,
): Unit = errorHandler.handleErrorMessage(error.message)
