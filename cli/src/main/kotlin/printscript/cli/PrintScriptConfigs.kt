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
import printscript.reader.JSONFormatterLanguageConfigReader
import printscript.reader.JSONFormatterRulesConfigReader
import printscript.reader.JSONGrammarConfigReader
import printscript.reader.JSONLanguageConfigReader
import printscript.reader.JSONLinterConfigReader
import printscript.reader.JSONTypeSystemConfigReader
import printscript.reader.YAMLFormatterRulesConfigReader
import printscript.util.fold
import usecases.LoadFormatter

internal class PrintScriptConfigs(
    val lang: LanguageConfig,
    val grammar: Grammar,
    val typeSystem: TypeSystemConfig,
    val linterConfig: LinterConfig,
    val formatter: Formatter,
) {
    companion object {
        private const val USER_YAML_PATH = ".printscript/formatter.yml"

        fun load(): PrintScriptConfigs {
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

        private fun userRules(): FormatterRulesConfig {
            val userYamlPath = Path.of(USER_YAML_PATH)
            return if (Files.exists(userYamlPath)) {
                YAMLFormatterRulesConfigReader.read(userYamlPath)
            } else {
                FormatterRulesConfig()
            }
        }

        private fun resource(name: String): InputStream =
            requireNotNull(JSONLanguageConfigReader::class.java.classLoader.getResourceAsStream(name)) {
                "Missing resource $name"
            }
    }
}
