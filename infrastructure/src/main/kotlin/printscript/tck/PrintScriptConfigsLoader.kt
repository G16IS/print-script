package printscript.tck

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import printscript.config.PrintScriptConfigs
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.reader.JSONFormatterLanguageConfigReader
import printscript.reader.JSONFormatterRulesConfigReader
import printscript.reader.JSONGrammarConfigReader
import printscript.reader.JSONLanguageConfigReader
import printscript.reader.JSONLinterConfigReader
import printscript.reader.JSONTypeSystemConfigReader
import printscript.reader.YAMLFormatterRulesConfigReader
import printscript.usecases.LoadFormatter
import printscript.util.fold

object PrintScriptConfigsLoader {
    private const val USER_YAML_PATH = ".printscript/formatter.yml"

    fun load(version: String): PrintScriptConfigs = load(version, userRules())

    /**
     * `formatter-user-defaults.json` queda siempre debajo de [user]: una rule que el
     * caller no manda toma el default interno, no queda sin definir.
     */
    fun load(
        version: String,
        user: FormatterRulesConfig,
    ): PrintScriptConfigs =
        loadConfigs(
            version,
            user,
            JSONFormatterRulesConfigReader.read(resource("formatter-user-defaults.json")),
        )

    private fun loadConfigs(
        version: String,
        user: FormatterRulesConfig,
        defaults: FormatterRulesConfig,
    ): PrintScriptConfigs {
        val lang =
            JSONLanguageConfigReader.read(resource("language.config.v$version.json"))

        val grammar =
            JSONGrammarConfigReader.read(resource("grammar.config.v$version.json"))

        val typeSystem =
            JSONTypeSystemConfigReader.read(resource("type-system.config.v$version.json"))

        val linterConfig =
            JSONLinterConfigReader.read(resource("linter.config.v$version.json"))

        return PrintScriptConfigs(
            lang = lang,
            grammar = grammar,
            typeSystem = typeSystem,
            linterConfig = linterConfig,
            formatter = loadFormatter(lang, grammar, version, user, defaults),
        )
    }

    private fun loadFormatter(
        lang: LanguageConfig,
        grammar: Grammar,
        version: String,
        user: FormatterRulesConfig,
        defaults: FormatterRulesConfig,
    ) = LoadFormatter
        .load(
            grammar,
            lang,
            JSONFormatterLanguageConfigReader.read(resource("formatter-language.v$version.json")),
            user,
            defaults,
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
