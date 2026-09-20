package printscript

import printscript.domain.LanguageConfig
import printscript.domain.Token
import printscript.error.LexerError
import printscript.reader.CodeReader
import printscript.util.Result

interface Lexer {
    companion object {
        fun create(
            codeReader: CodeReader,
            langConfig: LanguageConfig,
        ): Lexer = DefaultLexerFactory.create(codeReader, langConfig)
    }

    fun nextToken(): Result<Token, LexerError>

    fun peek(offset: Int = 0): Result<Token, LexerError>
}
