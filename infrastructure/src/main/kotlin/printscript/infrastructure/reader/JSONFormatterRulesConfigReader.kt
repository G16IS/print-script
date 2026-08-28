package printscript.infrastructure.reader

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import printscript.domain.FormatterRulesConfig
import printscript.infrastructure.serializer.config.FormatterRulesConfigSerializer
import printscript.reader.FormatterRulesConfigReader

/**
 * Carga `formatter-language.json` (reglas fijas de la versión).
 */
object JSONFormatterRulesConfigReader : FormatterRulesConfigReader {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    override fun read(path: Path): FormatterRulesConfig = read(path.readText())

    override fun read(input: InputStream): FormatterRulesConfig = read(input.bufferedReader().use { it.readText() })

    override fun read(text: String): FormatterRulesConfig = json.decodeFromString(FormatterRulesConfigSerializer, text)
}
