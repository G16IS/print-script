package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import printscript.domain.FormatterLanguageConfig

interface FormatterLanguageConfigReader {
    fun read(path: Path): FormatterLanguageConfig

    fun read(input: InputStream): FormatterLanguageConfig

    fun read(text: String): FormatterLanguageConfig
}
