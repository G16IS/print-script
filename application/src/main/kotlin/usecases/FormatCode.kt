package usecases

import java.nio.file.Path
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.FormatError
import printscript.util.Result
import printscript.util.flatMap

object FormatCode {
    fun formatCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        path: String,
        userYamlPath: Path = Path.of(LoadFormatter.USER_YAML_PATH),
    ): Result<String, FormatError> {
        val program = ParseProgram.parse(langConfig, grammar, path)

        return LoadFormatter.load(grammar, langConfig, userYamlPath).flatMap { formatter ->
            formatter.format(program)
        }
    }
}
