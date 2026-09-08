package usecases

import java.rmi.UnexpectedException
import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.Lexer
import printscript.Parser
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.Token
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.util.Result

internal object ParseProgram {
    fun parse(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
    ): SyntaxProgram {
        val lexer = DefaultLexerFactory.create(reader, langConfig)
        val parser = DefaultParserFactory.create(grammar)

        var program = SyntaxProgram.empty()

        while (peekNextToken(lexer).type != "EOF") {
            program = parseStatement(parser, lexer, program)
        }

        return program
    }

    private fun peekNextToken(lexer: Lexer): Token {
        when (val token = lexer.peek(null)) {
            is Result.Err -> throw UnexpectedException(token.error.message)
            is Result.Ok -> return token.value
        }
    }

    private fun parseStatement(
        parser: Parser,
        lexer: Lexer,
        program: SyntaxProgram,
    ): SyntaxProgram {
        when (val program = parser.parseNextStatement(lexer, program)) {
            is Result.Err -> throw UnexpectedException(program.error.message)
            is Result.Ok -> return program.value
        }
    }
}
