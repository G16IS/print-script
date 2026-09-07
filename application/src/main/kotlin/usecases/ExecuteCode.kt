package usecases

import printscript.DefaultInterpreterFactory
import printscript.InterpreterContext
import printscript.SideEffect
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.error.RuntimeError
import printscript.reader.CodeReader
import printscript.typechecker.DefaultTypeCheckerFactory
import printscript.typechecker.TypeError
import printscript.util.Result
import printscript.util.fold

object ExecuteCode {
    fun execute(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
    ): Result<List<SideEffect>, ExecutionFailure> {
        val program = ParseProgram.parse(langConfig, grammar, reader)
        val report = DefaultTypeCheckerFactory.create(typeSystem).check(program)

        if (!report.isOk) {
            return Result.Err(ExecutionFailure.Types(report.errors))
        }

        return DefaultInterpreterFactory
            .create()
            .interpret(InterpreterContext(), program)
            .fold(
                onOk = { Result.Ok(it) },
                onErr = { Result.Err(ExecutionFailure.Runtime(it)) },
            )
    }
}

sealed interface ExecutionFailure {
    data class Types(
        val errors: List<TypeError>,
    ) : ExecutionFailure

    data class Runtime(
        val error: RuntimeError,
    ) : ExecutionFailure
}
