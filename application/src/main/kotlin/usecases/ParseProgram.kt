package usecases

import java.rmi.UnexpectedException
import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.Lexer
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.Token
import printscript.infrastructure.reader.FileCodeReader
import printscript.syntax.SyntaxProgram
import printscript.util.Result

internal object ParseProgram {
    fun parse(
        langConfig: LanguageConfig,
        grammar: Grammar,
        path: String,
    ): SyntaxProgram {
        val codeReader = FileCodeReader(path)
        val lexer = DefaultLexerFactory.create(codeReader, langConfig)
        val parser = DefaultParserFactory.create(grammar)

        var program = SyntaxProgram.empty()

        while (peekNextToken(lexer).type != "EOF") {
            program = parser.parseNextStatement(lexer, program)
        }

        return program
    }

    private fun peekNextToken(lexer: Lexer): Token {
        when (val token = lexer.peek(null)) {
            is Result.Err -> throw UnexpectedException("Token error")
            is Result.Ok -> return token.value
        }
    }
}
