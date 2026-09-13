package printscript.usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.error.TypeErrorWithMessage
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.typechecker.DefaultTypeCheckerFactory
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
        kit: LanguageKit,
    ): Report<SyntaxProgram, Error> =
        when (val program = ParseProgram.parse(langConfig, grammar, reader, kit)) {
            is Result.Ok -> {
                val typeChecker = DefaultTypeCheckerFactory.create(typeSystem, kit.kindHandlerFactory)
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
