package printscript.cli

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.domain.TypeSystemConfig
import printscript.formatter.Formatter
import printscript.infrastructure.reader.JSONFormatterLanguageConfigReader
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.infrastructure.reader.JSONLanguageConfigReader
import printscript.infrastructure.reader.JSONLinterConfigReader
import printscript.infrastructure.reader.JSONTypeSystemConfigReader
import printscript.infrastructure.reader.YAMLFormatterRulesConfigReader
import printscript.util.fold
import usecases.LoadFormatter

class PrintScriptConfigs(
    val lang: LanguageConfig,
    val grammar: Grammar,
    val typeSystem: TypeSystemConfig,
    val linterConfig: LinterConfig,
    val formatter: Formatter,
) {
    companion object {
        const val USER_YAML_PATH = ".printscript/formatter.yml"

        fun load(userYamlPath: Path = Path.of(USER_YAML_PATH)): PrintScriptConfigs {
            val lang = JSONLanguageConfigReader.read(resource("language.config.json"))
            val grammar = JSONGrammarConfigReader.read(resource("grammar.config.json"))
            return PrintScriptConfigs(
                lang = lang,
                grammar = grammar,
                typeSystem = JSONTypeSystemConfigReader.read(resource("type-system.config.json")),
                linterConfig = JSONLinterConfigReader.read(resource("linter.config.json")),
                formatter = loadFormatter(lang, grammar, userYamlPath),
            )
        }

        private fun loadFormatter(
            lang: LanguageConfig,
            grammar: Grammar,
            userYamlPath: Path,
        ) = LoadFormatter
            .load(
                grammar,
                lang,
                JSONFormatterLanguageConfigReader.read(resource("formatter-language.json")),
                userRules(userYamlPath),
                JSONFormatterRulesConfigReader.read(resource("formatter-user-defaults.json")),
            ).fold(
                onOk = { it },
                onErr = { error("Could not load formatter: ${it.message}") },
            )

        private fun userRules(userYamlPath: Path) =
            if (Files.exists(userYamlPath)) {
                YAMLFormatterRulesConfigReader.read(userYamlPath)
            } else {
                FormatterRulesConfig()
            }

        private fun resource(name: String): InputStream =
            requireNotNull(JSONLanguageConfigReader::class.java.classLoader.getResourceAsStream(name)) {
                "Missing resource $name"
            }
    }
}
