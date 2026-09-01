package usecases

import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.FormatError
import printscript.util.Report
import printscript.util.fold

object CheckFormat {
    fun checkFormat(
        langConfig: LanguageConfig,
        grammar: Grammar,
        path: String,
        userYamlPath: Path = Path.of(LoadFormatter.USER_YAML_PATH),
    ): Report<Unit, FormatError> {
        val source =
            Files
                .readString(Path.of(path))
                .replace("\r\n", "\n")

        val program = ParseProgram.parse(langConfig, grammar, path)

        return LoadFormatter.load(grammar, langConfig, userYamlPath).fold(
            onOk = { formatter -> formatter.check(program, source) },
            onErr = { error -> Report(value = Unit, errors = listOf(error)) },
        )
    }
}
