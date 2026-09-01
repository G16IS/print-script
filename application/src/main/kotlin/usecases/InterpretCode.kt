package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxProgram
import printscript.typechecker.DefaultTypeCheckerFactory
import printscript.typechecker.TypeError
import printscript.util.Report

object InterpretCode {
    fun interpretCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        path: String,
    ): Report<SyntaxProgram, TypeError> {
        val program = ParseProgram.parse(langConfig, grammar, path)

        val report = DefaultTypeCheckerFactory.create(typeSystem).check(program)

        // TODO: Interpret the program here
        // TODO: Return a Result with the interpretation or an error if the interpretation fails

        return report
    }
}
