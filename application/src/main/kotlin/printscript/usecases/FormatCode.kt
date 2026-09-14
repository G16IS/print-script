package printscript.usecases

import java.io.Writer
import printscript.config.PrintScriptConfigs
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

    fun formatForTck(
        configs: PrintScriptConfigs,
        codeReader: CodeReader,
        writer: Writer,
        languageKit: LanguageKit,
    ) {
        when (
            val result =
                formatCode(
                    configs.lang,
                    configs.grammar,
                    codeReader,
                    configs.formatter,
                    languageKit,
                )
        ) {
            is Result.Ok -> writer.write(result.value)
            is Result.Err -> Unit
        }
    }
}
