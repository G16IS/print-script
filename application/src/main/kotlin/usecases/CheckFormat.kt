package usecases

import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.formatter.FormatError
import printscript.formatter.FormatterConfig
import printscript.util.fold

object CheckFormat {
    fun checkFormat(
        langConfig: LanguageConfig,
        grammar: Grammar,
        path: String,
        userYamlPath: Path = Path.of(FormatterConfig.USER_YAML_PATH),
    ) {
        val source =
            Files
                .readString(Path.of(path))
                .replace("\r\n", "\n")
                .trimEnd()

        val program = ParseProgram.parse(langConfig, grammar, path)
        val formatter = LoadFormatter.load(userYamlPath)

        val formatted =
            formatter
                .format(program)
                .fold(
                    onOk = { it.trimEnd() },
                    onErr = { error(formatFailed(it)) },
                )

        if (source != formatted) {
            error("El chequeo de formato falló: el archivo no está formateado")
        }
    }

    private fun formatFailed(error: FormatError): String {
        val position = error.location.start

        return "El formateo falló: ${error.message} @ ${position.line}:${position.col}"
    }
}
