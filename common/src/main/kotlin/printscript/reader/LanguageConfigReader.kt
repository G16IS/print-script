package printscript.reader

import printscript.domain.LanguageConfig
import java.io.InputStream
import java.nio.file.Path

interface LanguageConfigReader {
    fun read(path: Path): LanguageConfig
    fun read(input: InputStream): LanguageConfig
    fun read(jsonString: String): LanguageConfig
}