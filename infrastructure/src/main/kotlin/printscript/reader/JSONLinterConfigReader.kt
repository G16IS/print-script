package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import printscript.domain.LinterConfig
import printscript.serializer.config.LinterConfigSerializer

object JSONLinterConfigReader : LinterConfigReader {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    override fun read(path: Path): LinterConfig = read(path.readText())

    override fun read(input: InputStream): LinterConfig = read(input.bufferedReader().use { it.readText() })

    override fun read(jsonString: String): LinterConfig = json.decodeFromString(LinterConfigSerializer, jsonString)
}
