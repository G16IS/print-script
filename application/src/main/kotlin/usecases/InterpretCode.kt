package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.typechecker.DefaultTypeCheckerFactory
import printscript.typechecker.TypeError
import printscript.util.Report

object InterpretCode {
    fun interpretCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
        onStatement: () -> Unit = {},
    ): Report<SyntaxProgram, TypeError> {
        val program = ParseProgram.parse(langConfig, grammar, reader, onStatement)

        return DefaultTypeCheckerFactory.create(typeSystem).check(program)
    }
}
