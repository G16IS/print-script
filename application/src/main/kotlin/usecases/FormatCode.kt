package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.FormatError
import printscript.formatter.Formatter
import printscript.reader.CodeReader
import printscript.util.Result

object FormatCode {
    fun formatCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        formatter: Formatter,
    ): Result<String, FormatError> {
        val program = ParseProgram.parse(langConfig, grammar, reader)

        return formatter.format(program)
    }
}
