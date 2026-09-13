package printscript.usecases

import printscript.DefaultInterpreterFactory
import printscript.ErrorHandler
import printscript.InterpreterContext
import printscript.SideEffect
import printscript.SideEffectManager
import printscript.config.PrintScriptConfigs
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageCatalog
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.error.FormatError
import printscript.error.LexerError
import printscript.error.LintError
import printscript.error.ParserError
import printscript.error.RuntimeError
import printscript.error.TypeError
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
        kit: LanguageKit = LanguageCatalog.v10,
    ): Result<List<SideEffect>, ExecutionFailure> {
        val report = TypecheckCode.typecheck(langConfig, grammar, typeSystem, reader, kit)

        if (!report.isOk) {
            return Result.Err(ExecutionFailure.Types(report.errors))
        }

        return DefaultInterpreterFactory
            .create(kit.evaluators, kit.executors)
            .interpret(InterpreterContext(), report.value!!)
            .fold(
                onOk = { Result.Ok(it) },
                onErr = { Result.Err(ExecutionFailure.Runtime(it)) },
            )
    }

    @Suppress("UnusedParameter")
    fun executeForTck(
        configs: PrintScriptConfigs,
        codeReader: CodeReader,
        sideEffectManager: SideEffectManager,
        errorHandler: ErrorHandler,
        languageKit: LanguageKit,
    ) {
        val report = TypecheckCode.typecheck(configs.lang, configs.grammar, configs.typeSystem, codeReader, languageKit)

        if (!report.isOk) {
            report.errors.forEach { reportError(it, errorHandler) }
        }

        val evaluators = languageKit.evaluators
        val statementExecutors = languageKit.executors

        val interpreterResult: Result<List<SideEffect>, RuntimeError> =
            DefaultInterpreterFactory
                .create(evaluators, statementExecutors, sideEffectManager)
                .interpret(InterpreterContext(), report.value!!)

        if (interpreterResult is Result.Err) reportError((interpreterResult).error, errorHandler)
    }
}

private fun reportError(
    error: Error,
    errorHandler: ErrorHandler,
) {
    errorHandler.handleErrorMessage(error.toMessage())
}

private fun Error.toMessage(): String =
    when (this) {
        is FormatError -> message
        is LexerError -> message
        is LintError -> message
        is ParserError -> message
        is RuntimeError -> message
        is TypeError -> message
    }

sealed interface ExecutionFailure {
    data class Types(
        val errors: List<Error>,
    ) : ExecutionFailure

    data class Runtime(
        val error: RuntimeError,
    ) : ExecutionFailure
}
