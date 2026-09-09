package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.Error
import printscript.formatter.Formatter
import printscript.reader.CodeReader
import printscript.util.Report
import printscript.util.Result

object CheckFormat {
    fun checkFormat(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        source: String,
        formatter: Formatter,
    ): Report<Unit, Error> {
        val program =
            when (
                val result = ParseProgram.parse(langConfig, grammar, reader)
            ) {
                is Result.Err -> return Report(errors = listOf(result.error))
                is Result.Ok -> result.value
            }

        val checked = formatter.check(program, source.replace("\r\n", "\n"))
        return Report(value = checked.value, errors = checked.errors)
    }
}
