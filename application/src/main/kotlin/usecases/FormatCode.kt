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
        onStatement: () -> Unit = {},
    ): Result<String, FormatError> {
        val program = ParseProgram.parse(langConfig, grammar, reader, onStatement)

        return formatter.format(program)
    }
}
