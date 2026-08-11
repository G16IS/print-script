package printscript.lexer

import printscript.common.reader.CodeReader

interface Lexer {
    fun nextToken(): Token
    fun peek(offset: Int?): Token
}
