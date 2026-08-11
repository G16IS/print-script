package printscript.formatreader

import printscript.common.reader.CodeReader
import printscript.lexer.Token

interface FormatReader {
    fun read(firstChar: Char, reader: CodeReader): Token
}
