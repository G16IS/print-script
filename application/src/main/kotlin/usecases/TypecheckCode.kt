package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.error.Error
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.typechecker.DefaultTypeCheckerFactory
import printscript.util.Report
import printscript.util.Result

object TypecheckCode {
    /**
     * Typechecks the code read from the given reader
     * using the provided language configuration,
     * grammar, and type system configuration.
     *
     * @param langConfig The language configuration to use for typechecking.
     * @param grammar The grammar to use for parsing the code.
     * @param typeSystem The type system configuration to use for typechecking.
     * @param reader The code reader to read the code from.
     * @return A report containing either the successfully parsed
     * and typechecked program or a list of type errors encountered during typechecking.
     */
    fun typecheck(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        reader: CodeReader,
    ): Report<SyntaxProgram, Error> {
        return when (val program = ParseProgram.parse(langConfig, grammar, reader)) {
            is Result.Ok -> DefaultTypeCheckerFactory
                .create(typeSystem)
                .check(program.value)
            is Result.Err -> program.toReport()
        }
    }
}
