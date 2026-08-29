package usecases

import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TokenLexemes
import printscript.formatter.DefaultFormatterFactory
import printscript.formatter.Formatter
import printscript.formatter.FormatterConfig
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.infrastructure.reader.YAMLFormatterRulesConfigReader
import printscript.util.fold

internal object LoadFormatter {
    fun load(
        grammar: Grammar,
        langConfig: LanguageConfig,
        userYamlPath: Path = Path.of(FormatterConfig.USER_YAML_PATH),
    ): Formatter {
        val language =
            JSONFormatterRulesConfigReader.read(
                requireNotNull(
                    javaClass.classLoader.getResourceAsStream(FormatterConfig.LANGUAGE_JSON_RESOURCE),
                ) {
                    "Missing resource ${FormatterConfig.LANGUAGE_JSON_RESOURCE}"
                },
            )

        val user =
            if (Files.exists(userYamlPath)) {
                YAMLFormatterRulesConfigReader.read(userYamlPath)
            } else {
                FormatterRulesConfig()
            }

        val lexemes = TokenLexemes.from(langConfig)

        return DefaultFormatterFactory
            .createFromConfig(language, user, grammar, lexemes)
            .fold(
                onOk = { it },
                onErr = { error(it.message) },
            )
    }
}
