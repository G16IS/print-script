package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import printscript.domain.TypeSystemConfig

interface TypeSystemConfigReader {
    fun read(path: Path): TypeSystemConfig

    fun read(input: InputStream): TypeSystemConfig

    fun read(jsonString: String): TypeSystemConfig
}
