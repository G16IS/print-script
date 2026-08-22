package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import printscript.domain.LanguageConfig

interface LanguageConfigReader {
    fun read(path: Path): LanguageConfig

    fun read(input: InputStream): LanguageConfig

    fun read(jsonString: String): LanguageConfig
}
