package printscript.reader

import printscript.domain.Grammar
import java.io.InputStream
import java.nio.file.Path

interface GrammarConfigReader {
    fun read(path: Path): Grammar
    fun read(input: InputStream): Grammar
    fun read(jsonString: String): Grammar
}
