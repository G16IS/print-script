package printscript.readers

import printscript.common.domain.Token
import printscript.common.reader.CodeReader

interface FormatReader {
    fun read(firstChar: Char, reader: CodeReader): Token
}
