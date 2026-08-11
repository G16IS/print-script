package printscript.readers

import printscript.domain.Token
import printscript.reader.CodeReader

interface FormatReader {
    fun read(firstChar: Char, reader: CodeReader): Token
}
