package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.FormatError
import printscript.formatter.Formatter
import printscript.reader.CodeReader
import printscript.util.Report

object CheckFormat {
    fun checkFormat(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        source: String,
        formatter: Formatter,
        onStatement: () -> Unit = {},
    ): Report<Unit, FormatError> {
        val program = ParseProgram.parse(langConfig, grammar, reader, onStatement)

        return formatter.check(program, source.replace("\r\n", "\n"))
    }
}
