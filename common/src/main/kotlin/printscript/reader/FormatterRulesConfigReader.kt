package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import printscript.domain.FormatterRulesConfig

interface FormatterRulesConfigReader {
    fun read(path: Path): FormatterRulesConfig

    fun read(input: InputStream): FormatterRulesConfig

    fun read(text: String): FormatterRulesConfig
}
