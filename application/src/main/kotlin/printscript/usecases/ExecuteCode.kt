package printscript.usecases

import printscript.DefaultInterpreterFactory
import printscript.Interpreter
import printscript.InterpreterContext
import printscript.config.PrintScriptConfigs
import printscript.definitions.ErrorHandler
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.reader.CodeReader
import printscript.syntax.SyntaxNode
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeChecker
import printscript.util.Result
import printscript.util.err
import printscript.util.flatMap
import printscript.util.isOk
import printscript.util.map
import printscript.util.mapError
import printscript.util.ok
import printscript.util.safe
import printscript.util.unwrapErr

object ExecuteCode {
    fun execute(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        kit: LanguageKit,
    ): Result<Unit, Error> =
        safe {
            val typeChecker = TypeChecker.create(typeSystem, kit.kindHandlerFactory)
            val interpreter = DefaultInterpreterFactory.create(kit.evaluators, kit.executors)

            var scope = ScopeStack()
            var context = InterpreterContext()

            for (parsed in ParseProgram.parseStatements(langConfig, grammar, reader, kit)) {
                val result =
                    parsed.flatMap { node ->
                        checkAndExecute(
                            node,
                            typeChecker,
                            interpreter,
                            scope,
                            context,
                        ) { newScope, newContext ->
                            scope = newScope
                            context = newContext
                        }
                    }

                if (!result.isOk) {
                    return@safe err(result.unwrapErr())
                }
            }

            ok(Unit)
        }

    fun executeForTck(
        configs: PrintScriptConfigs,
        codeReader: CodeReader,
        errorHandler: ErrorHandler,
        languageKit: LanguageKit,
    ) {
        val result = execute(configs.lang, configs.grammar, configs.typeSystem, codeReader, languageKit)

        if (!result.isOk) {
            reportError(
                result.unwrapErr(),
                errorHandler,
            )
        }
    }

    private fun checkAndExecute(
        statement: SyntaxNode,
        typeChecker: TypeChecker,
        interpreter: Interpreter,
        scope: ScopeStack,
        context: InterpreterContext,
        onSuccess: (ScopeStack, InterpreterContext) -> Unit,
    ): Result<Unit, Error> =
        when (val checked = typeChecker.checkNode(statement, scope)) {
            is Result.Err -> err(checked.error.error)
            is Result.Ok ->
                interpreter
                    .executeStatement(checked.value.second, context)
                    .asError()
                    .map { newContext ->
                        onSuccess(checked.value.first, newContext)
                    }
        }
}

private fun <T, E : Error> Result<T, E>.asError(): Result<T, Error> = mapError { it }

private fun reportError(
    error: Error,
    errorHandler: ErrorHandler,
): Unit = errorHandler.handleErrorMessage(error.message)
