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
import printscript.error.ExecutionFailure
import printscript.reader.CodeReader
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
    ): Result<Unit, ExecutionFailure> {
        val report = TypecheckCode.typecheck(langConfig, grammar, typeSystem, reader, kit)

        if (!report.isOk) {
            return Result.Err(ExecutionFailure.Types(report.errors))
        }

        return DefaultInterpreterFactory
            .create(kit.evaluators, kit.executors)
            .interpret(InterpreterContext(), report.value!!)
            .fold(
                onOk = { Result.Ok(Unit) },
                onErr = { Result.Err(ExecutionFailure.Runtime(it)) },
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
            report.errors.forEach { reportError(it) }
            errorHandler.handleErrorMessage("Typechecking failed with errors: ${report.errors}")
        }

        val evaluators = languageKit.evaluators
        val statementExecutors = languageKit.executors

        val interpreterResult =
            DefaultInterpreterFactory
                .create(evaluators, statementExecutors)
                .interpret(InterpreterContext(), report.value!!)

        if (interpreterResult is Result.Err) reportError((interpreterResult).error)
    }
}

private fun reportError(error: Error): Unit =
    throw IllegalArgumentException("Error reporting is not implemented yet. Error: $error")
