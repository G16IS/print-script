package printscript

import printscript.common.domain.Token

interface Lexer {
    fun nextToken(): Token
    fun peek(offset: Int?): Token
}