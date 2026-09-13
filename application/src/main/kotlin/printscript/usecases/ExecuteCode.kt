package printscript.usecases

import printscript.DefaultInterpreterFactory
import printscript.ErrorHandler
import printscript.InputChannel
import printscript.InterpreterContext
import printscript.PrintChannel
import printscript.SideEffect
import printscript.config.PrintScriptConfigs
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.error.RuntimeError
import printscript.reader.CodeReader
import printscript.util.Result
import printscript.util.fold

object ExecuteCode {
    fun execute(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        kit: LanguageKit,
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
        version: String,
        configs: PrintScriptConfigs,
        codeReader: CodeReader,
        printChannel: PrintChannel,
        errorHandler: ErrorHandler,
        inputChannel: InputChannel,
    ) {
        TODO("implement but have to change module factories to instanciate based on the version")
    }
}

sealed interface ExecutionFailure {
    data class Types(
        val errors: List<Error>,
    ) : ExecutionFailure

    data class Runtime(
        val error: RuntimeError,
    ) : ExecutionFailure
}
