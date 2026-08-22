package printscript

import printscript.domain.Token

interface Lexer {
    fun nextToken(): Token

    fun peek(offset: Int?): Token
}
