package printscript.usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.reader.CodeReader
import printscript.syntax.SyntaxNode
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
        val builder = SyntaxProgram.builder()
        val errors =
            walk(langConfig, grammar, typeSystem, reader, kit) { statement ->
                builder.add(statement)
            }
        return Report(value = builder.build(), errors = errors)
    }

    fun check(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        kit: LanguageKit,
    ): Report<Unit, Error> =
        Report(
            value = Unit,
            errors = walk(langConfig, grammar, typeSystem, reader, kit) {},
        )

    private fun walk(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        kit: LanguageKit,
        onStatement: (SyntaxNode) -> Unit,
    ): List<Error> {
        val typeChecker = TypeChecker.create(typeSystem, kit.kindHandlerFactory)
        val errors = mutableListOf<Error>()
        var scope = ScopeStack()

        for (parsed in ParseProgram.parseStatements(langConfig, grammar, reader, kit)) {
            when (parsed) {
                is Result.Err -> return errors + parsed.error
                is Result.Ok -> {
                    onStatement(parsed.value)
                    scope = record(typeChecker, parsed.value, scope, errors)
                }
            }
        }

        return errors
    }

    private fun record(
        typeChecker: TypeChecker,
        statement: SyntaxNode,
        scope: ScopeStack,
        errors: MutableList<Error>,
    ): ScopeStack =
        when (val checked = typeChecker.checkNode(statement, scope)) {
            is Result.Ok -> checked.value.first
            is Result.Err -> {
                errors += checked.error.error
                checked.error.scope
            }
        }
}
