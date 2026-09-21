package printscript.usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.error.TypeErrorWithMessage
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeChecker
import printscript.util.Report
import printscript.util.Result

object TypecheckCode {
    fun typecheck(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        kit: LanguageKit,
    ): Report<SyntaxProgram, Error> {
        val typeChecker = TypeChecker.create(typeSystem, kit.kindHandlerFactory)
        val builder = SyntaxProgram.builder()
        val errors = mutableListOf<Error>()
        var scope = ScopeStack()

        for (parsed in ParseProgram.parseStatements(langConfig, grammar, reader, kit)) {
            when (parsed) {
                is Result.Err -> return Report(errors = errors + parsed.error)
                is Result.Ok -> {
                    val stmt = parsed.value
                    builder.add(stmt)
                    val checked = typeChecker.checkStatement(stmt, scope)
                    errors += checked.errors.map { TypeErrorWithMessage(it.message, it.location) }
                    scope = checked.scope
                }
            }
        }

        return Report(value = builder.build(), errors = errors)
    }

    fun check(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        kit: LanguageKit,
    ): Report<Unit, Error> {
        val typeChecker = TypeChecker.create(typeSystem, kit.kindHandlerFactory)
        val errors = mutableListOf<Error>()
        var scope = ScopeStack()

        for (parsed in ParseProgram.parseStatements(langConfig, grammar, reader, kit)) {
            when (parsed) {
                is Result.Err -> return Report(errors = errors + parsed.error)
                is Result.Ok -> {
                    val stmt = parsed.value
                    val checked = typeChecker.checkStatement(stmt, scope)
                    errors += checked.errors.map { TypeErrorWithMessage(it.message, it.location) }
                    scope = checked.scope
                }
            }
        }

        return Report(value = Unit, errors = errors)
    }
}
