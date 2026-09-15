package printscript.usecases

import printscript.DefaultInterpreterFactory
import printscript.ErrorHandler
import printscript.Interpreter
import printscript.InterpreterContext
import printscript.config.PrintScriptConfigs
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.error.TypeErrorWithMessage
import printscript.reader.CodeReader
import printscript.syntax.SyntaxNode
import printscript.typechecker.DefaultTypeCheckerFactory
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeChecker
import printscript.util.Report
import printscript.util.Result

object ExecuteCode {
    fun execute(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        kit: LanguageKit,
    ): Report<Unit, Error> {
        val typeChecker = DefaultTypeCheckerFactory.create(typeSystem, kit.kindHandlerFactory)
        val interpreter = DefaultInterpreterFactory.create(kit.evaluators, kit.executors)

        var scope = ScopeStack()
        var context = InterpreterContext()
        var errorReport: Report<Unit, Error>? = null

        for (parsed in ParseProgram.parseStatements(langConfig, grammar, reader, kit)) {
            val failure =
                executeStatement(parsed, typeChecker, interpreter, scope, context) { newScope, newContext ->
                    scope = newScope
                    context = newContext
                }
            if (failure != null) {
                errorReport = failure
                break
            }
        }

        return errorReport ?: Report(value = Unit)
    }

    fun executeForTck(
        configs: PrintScriptConfigs,
        codeReader: CodeReader,
        errorHandler: ErrorHandler,
        languageKit: LanguageKit,
    ) {
        val report = execute(configs.lang, configs.grammar, configs.typeSystem, codeReader, languageKit)
        if (!report.isOk) {
            report.errors.forEach { reportError(it, errorHandler) }
        }
    }

    private fun executeStatement(
        parsed: Result<SyntaxNode, Error>,
        typeChecker: TypeChecker,
        interpreter: Interpreter,
        scope: ScopeStack,
        context: InterpreterContext,
        onSuccess: (ScopeStack, InterpreterContext) -> Unit,
    ): Report<Unit, Error>? =
        when (parsed) {
            is Result.Err -> Report(errors = listOf(parsed.error))
            is Result.Ok -> checkAndExecute(parsed.value, typeChecker, interpreter, scope, context, onSuccess)
        }

    private fun checkAndExecute(
        statement: SyntaxNode,
        typeChecker: TypeChecker,
        interpreter: Interpreter,
        scope: ScopeStack,
        context: InterpreterContext,
        onSuccess: (ScopeStack, InterpreterContext) -> Unit,
    ): Report<Unit, Error>? {
        val checked = typeChecker.checkStatement(statement, scope)
        if (checked.errors.isNotEmpty()) {
            return Report(
                errors = checked.errors.map { TypeErrorWithMessage(it.message, it.location) },
            )
        }

        return when (val execResult = interpreter.executeStatement(statement, context)) {
            is Result.Err -> Report(errors = listOf(execResult.error))
            is Result.Ok -> {
                onSuccess(checked.scope, execResult.value)
                null
            }
        }
    }
}

private fun reportError(
    error: Error,
    errorHandler: ErrorHandler,
): Unit = errorHandler.handleErrorMessage(error.message)
