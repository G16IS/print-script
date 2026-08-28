package usecases

import java.nio.file.Path
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.formatter.FormatError
import printscript.formatter.FormatterConfig
import printscript.util.fold

object FormatCode {
    fun formatCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        path: String,
        userYamlPath: Path = Path.of(FormatterConfig.USER_YAML_PATH),
    ): String {
        val program = ParseProgram.parse(langConfig, grammar, path)
        val formatter = LoadFormatter.load(userYamlPath)

        return formatter
            .format(program)
            .fold(
                onOk = { it },
                onErr = { error(formatFailed(it)) },
            )
    }

    private fun formatFailed(error: FormatError): String {
        val position = error.location.start

        return "El formateo falló: ${error.message} @ ${position.line}:${position.col}"
    }
}
