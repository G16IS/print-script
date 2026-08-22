package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import printscript.domain.Grammar

interface GrammarConfigReader {
    fun read(path: Path): Grammar

    fun read(input: InputStream): Grammar

    fun read(jsonString: String): Grammar
}
