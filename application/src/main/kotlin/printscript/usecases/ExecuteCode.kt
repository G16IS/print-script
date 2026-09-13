package printscript.usecases

import printscript.DefaultInterpreterFactory
import printscript.ErrorHandler
import printscript.InputChannel
import printscript.Interpreter
import printscript.InterpreterContext
import printscript.PrintChannel
import printscript.PrintEffect
import printscript.SideEffect
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
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Report
import printscript.util.Result
import printscript.util.fold

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
        version: String,
        configs: PrintScriptConfigs,
        codeReader: CodeReader,
        printChannel: PrintChannel,
        errorHandler: ErrorHandler,
        inputChannel: InputChannel,
    ): Result<Unit, ExecutionFailure> {
        val report = TypecheckCode.typecheck(configs.lang, configs.grammar, configs.typeSystem, codeReader, version)

        if (!report.isOk) {
            report.errors.forEach { reportError(it, errorHandler) }
            return Result.Err(ExecutionFailure.Types(report.errors))
        }

        return InterpreterFactory
            .create(version)
            .fold(
                onOk = { interpreter ->
                    interpretByLine(interpreter, report, printChannel, errorHandler)
                },
                onErr = { error ->
                    reportError(error, errorHandler)
                    Result.Err(ExecutionFailure.Runtime(error))
                },
            )
    }
}

private fun interpretByLine(
    interpreter: Interpreter,
    report: Report<SyntaxProgram, Error>,
    printChannel: PrintChannel,
    errorHandler: ErrorHandler,
): Result<Unit, ExecutionFailure.Runtime> {
    // TODO: Finish this implementation
    val statements: List<SyntaxNode> = report.value!!.statements
    var context = InterpreterContext()

    for (statement in statements) {
        interpreter.executeStatement(statement, context).fold(
            onOk = { statementResult ->
                statementResult.sideEffects.forEach { runEffect(it, printChannel) }
            },
            onErr = { error ->
                reportError(error, errorHandler)
                Result.Err(ExecutionFailure.Runtime(error))
            },
        )
    }
    return Result.Ok(Unit)
}

private fun runEffect(
    effect: SideEffect,
    printChannel: PrintChannel,
) {
    when (effect) {
        is PrintEffect -> printChannel.print(effect.text)
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
