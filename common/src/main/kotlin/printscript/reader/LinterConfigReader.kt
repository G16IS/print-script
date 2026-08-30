package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import printscript.domain.LinterConfig

interface LinterConfigReader {
    fun read(path: Path): LinterConfig

    fun read(input: InputStream): LinterConfig

    fun read(jsonString: String): LinterConfig
}
