package printscript.infrastructure.cli

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.infrastructure.reader.JSONFormatterLanguageConfigReader
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.infrastructure.reader.JSONLanguageConfigReader
import printscript.infrastructure.reader.JSONLinterConfigReader
import printscript.infrastructure.reader.JSONTypeSystemConfigReader
import printscript.infrastructure.reader.YAMLFormatterRulesConfigReader
import printscript.util.fold
import usecases.LoadFormatter

class DefaultConfigFactory(
    private val userYamlPath: Path = Path.of(USER_YAML_PATH),
) : ConfigFactory {
    override fun load(): PrintScriptConfigs {
        val lang = JSONLanguageConfigReader.read(resource("language.config.json"))
        val grammar = JSONGrammarConfigReader.read(resource("grammar.config.json"))
        return PrintScriptConfigs(
            lang = lang,
            grammar = grammar,
            typeSystem = JSONTypeSystemConfigReader.read(resource("type-system.config.json")),
            linterConfig = JSONLinterConfigReader.read(resource("linter.config.json")),
            formatter = loadFormatter(lang, grammar),
        )
    }

    private fun loadFormatter(
        lang: LanguageConfig,
        grammar: Grammar,
    ) = LoadFormatter
        .load(
            grammar,
            lang,
            JSONFormatterLanguageConfigReader.read(resource("formatter-language.json")),
            userRules(),
            JSONFormatterRulesConfigReader.read(resource("formatter-user-defaults.json")),
        ).fold(
            onOk = { it },
            onErr = { error("Could not load formatter: ${it.message}") },
        )

    private fun userRules() =
        if (Files.exists(userYamlPath)) {
            YAMLFormatterRulesConfigReader.read(userYamlPath)
        } else {
            FormatterRulesConfig()
        }

    private fun resource(name: String): InputStream =
        requireNotNull(DefaultConfigFactory::class.java.classLoader.getResourceAsStream(name)) {
            "Missing resource $name"
        }

    companion object {
        const val USER_YAML_PATH = ".printscript/formatter.yml"
    }
}
