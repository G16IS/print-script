package printscript.usecases

import printscript.ErrorHandler
import printscript.InputChannel
import printscript.InterpreterContext
import printscript.PrintChannel
import printscript.SideEffect
import printscript.config.PrintScriptConfigs
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.error.Error
import printscript.error.RuntimeError
import printscript.factory.InterpreterFactory
import printscript.reader.CodeReader
import printscript.util.Result
import printscript.util.fold

object ExecuteCode {
    fun execute(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
    ): Result<List<SideEffect>, ExecutionFailure> {
        val report = TypecheckCode.typecheck(langConfig, grammar, typeSystem, reader)

        if (!report.isOk) {
            return Result.Err(ExecutionFailure.Types(report.errors))
        }

        return InterpreterFactory
            .create("1")
            .fold(
                onOk = { interpreter ->
                    interpreter
                        .interpret(InterpreterContext(), report.value!!)
                        .fold(
                            onOk = { Result.Ok(it) },
                            onErr = { Result.Err(ExecutionFailure.Runtime(it)) },
                        )
                },
                onErr = { Result.Err(ExecutionFailure.Runtime(it)) },
            )
    }

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
