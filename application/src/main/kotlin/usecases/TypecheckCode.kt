package usecases

import printscript.application.factory.typechecker.TypeCheckerFactory
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.error.Error
import printscript.error.TypeErrorWithMessage
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.typechecker.TypeChecker
import printscript.util.Report
import printscript.util.Result
import printscript.util.toReport

object TypecheckCode {
    fun typecheck(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        version: String = "1",
    ): Report<SyntaxProgram, Error> =
        when (val program = ParseProgram.parse(langConfig, grammar, reader, version)) {
            is Result.Ok -> {
                val typeCheckerFactoryResult = TypeCheckerFactory.create(typeSystem, version)
                val typeChecker: TypeChecker =
                    when (typeCheckerFactoryResult) {
                        is Result.Err -> return typeCheckerFactoryResult.toReport()
                        is Result.Ok -> typeCheckerFactoryResult.value
                    }
                checkTypes(typeChecker, program.value)
            }
            is Result.Err -> program.toReport()
        }

    private fun checkTypes(
        typeChecker: TypeChecker,
        program: SyntaxProgram,
    ): Report<SyntaxProgram, Error> {
        val result = typeChecker.check(program)
        return Report(
            value = result.value,
            errors = result.errors.map { TypeErrorWithMessage(it.message, it.location) },
        )
    }
}
