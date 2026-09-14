package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import printscript.domain.TypeSystemConfig
import printscript.serializer.config.TypeSystemConfigSerializer

/**
 * Carga y deserializa archivos `type-system.config.json`.
 */
object JSONTypeSystemConfigReader : TypeSystemConfigReader {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    override fun read(path: Path): TypeSystemConfig = read(path.readText())

    override fun read(input: InputStream): TypeSystemConfig = read(input.bufferedReader().use { it.readText() })

    override fun read(jsonString: String): TypeSystemConfig =
        json.decodeFromString(TypeSystemConfigSerializer, jsonString)
}
