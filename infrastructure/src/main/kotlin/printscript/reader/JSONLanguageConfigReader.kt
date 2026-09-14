package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import printscript.domain.ExactRule
import printscript.domain.LanguageConfig
import printscript.domain.RegexRule
import printscript.serializer.config.LanguageConfigSerializer

object JSONLanguageConfigReader : LanguageConfigReader {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    override fun read(path: Path): LanguageConfig = read(path.readText())

    override fun read(input: InputStream): LanguageConfig = read(input.bufferedReader().use { it.readText() })

    override fun read(jsonString: String): LanguageConfig {
        val config = json.decodeFromString(LanguageConfigSerializer, jsonString)
        validate(config)
        return config
    }

    /**
     * Validaciones de consistencia post-deserialización.
     */
    private fun validate(config: LanguageConfig) {
        require(config.order.isNotEmpty()) { "order no puede estar vacío" }

        val missing = config.order.filter { it !in config.config }
        require(missing.isEmpty()) {
            "Categorías en order no definidas en config: $missing"
        }

        config.config.forEach { (category, rules) ->
            require(rules.isNotEmpty()) { "Categoría '$category' no tiene reglas" }

            rules.forEachIndexed { index, rule ->
                require(rule.matcher.isNotEmpty()) {
                    "Regla $index de '$category' no tiene matchers"
                }
                require(rule.token.isNotBlank()) {
                    "Regla $index de '$category' tiene token vacío"
                }

                when (rule) {
                    is RegexRule -> {
                        require(rule.partial.isNotBlank()) {
                            "RegexRule en '$category'[$index] debe definir 'partial'"
                        }
                    }

                    is ExactRule -> { // ok
                    }
                }
            }
        }
    }
}
