package printscript

import printscript.domain.Token
import printscript.error.LexerError
import printscript.util.Result

interface Lexer {
    fun nextToken(): Result<Token, LexerError>

    fun peek(offset: Int?): Result<Token, LexerError>
}
