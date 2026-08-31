package usecases

import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.formatter.FormatError

object CheckFormat {
    fun checkFormat(
        langConfig: LanguageConfig,
        grammar: Grammar,
        path: String,
        userYamlPath: Path = Path.of(LoadFormatter.USER_YAML_PATH),
    ) {
        val source =
            Files
                .readString(Path.of(path))
                .replace("\r\n", "\n")

        val program = ParseProgram.parse(langConfig, grammar, path)
        val formatter = LoadFormatter.load(grammar, langConfig, userYamlPath)
        val report = formatter.check(program, source)

        if (!report.isOk) {
            failFormatCheck(report.errors)
        }
    }

    private fun failFormatCheck(errors: List<FormatError>): Nothing {
        val messages =
            errors.joinToString("\n") { formatError ->
                val position = formatError.location.start
                "  - ${formatError.message} @ ${position.line}:${position.col}"
            }
        error("El chequeo de formato falló:\n$messages")
    }
}
