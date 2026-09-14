package printscript.usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.formatter.Formatter
import printscript.reader.CodeReader
import printscript.util.Result

object FormatCode {
    fun formatCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        formatter: Formatter,
        kit: LanguageKit,
    ): Result<String, Error> =
        when (
            val program = ParseProgram.parse(langConfig, grammar, reader, kit)
        ) {
            is Result.Ok -> formatter.format(program.value)
            is Result.Err -> Result.Err(program.error)
        }
}
