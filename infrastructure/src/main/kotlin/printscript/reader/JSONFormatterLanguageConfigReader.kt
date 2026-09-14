package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import printscript.domain.FormatterLanguageConfig
import printscript.serializer.config.FormatterLanguageConfigSerializer

/**
 * Carga `formatter-language.json` (rules fijas + bindings de types de usuario).
 */
object JSONFormatterLanguageConfigReader : FormatterLanguageConfigReader {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    override fun read(path: Path): FormatterLanguageConfig = read(path.readText())

    override fun read(input: InputStream): FormatterLanguageConfig = read(input.bufferedReader().use { it.readText() })

    override fun read(text: String): FormatterLanguageConfig =
        json.decodeFromString(FormatterLanguageConfigSerializer, text)
}
