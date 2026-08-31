package usecases

import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TokenLexemes
import printscript.formatter.DefaultFormatterFactory
import printscript.formatter.Formatter
import printscript.infrastructure.reader.JSONFormatterLanguageConfigReader
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.infrastructure.reader.YAMLFormatterRulesConfigReader
import printscript.util.fold

internal object LoadFormatter {
    const val USER_YAML_PATH = ".printscript/formatter.yml"

    private const val LANGUAGE_JSON_RESOURCE = "formatter-language.json"
    private const val USER_DEFAULTS_JSON_RESOURCE = "formatter-user-defaults.json"

    fun load(
        grammar: Grammar,
        langConfig: LanguageConfig,
        userYamlPath: Path = Path.of(USER_YAML_PATH),
    ): Formatter {
        val language = JSONFormatterLanguageConfigReader.read(resource(LANGUAGE_JSON_RESOURCE))
        val defaults = JSONFormatterRulesConfigReader.read(resource(USER_DEFAULTS_JSON_RESOURCE))
        val user =
            if (Files.exists(userYamlPath)) {
                YAMLFormatterRulesConfigReader.read(userYamlPath)
            } else {
                FormatterRulesConfig()
            }

        val lexemes = TokenLexemes.from(langConfig)

        return DefaultFormatterFactory
            .createFromConfig(language, user, defaults, grammar, lexemes)
            .fold(
                onOk = { it },
                onErr = { error(it.message) },
            )
    }

    private fun resource(name: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(name)) {
            "Missing resource $name"
        }
}
