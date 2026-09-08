package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.Error
import printscript.formatter.Formatter
import printscript.reader.CodeReader
import printscript.util.Report
import printscript.util.Result
import printscript.util.toReport

object CheckFormat {
    fun checkFormat(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        source: String,
        formatter: Formatter,
    ): Report<Unit, out Error> {
        val program = when (
            val result = ParseProgram.parse(langConfig, grammar, reader)
        ) {
            is Result.Err -> return result.toReport()
            is Result.Ok -> result.value
        }

        return formatter.check(program, source.replace("\r\n", "\n"))
    }
}
