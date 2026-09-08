package usecases

import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.error.Error
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram
import printscript.util.Result

internal object ParseProgram {
    fun parse(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
    ): Result<SyntaxProgram, Error> {
        val lexer = DefaultLexerFactory.create(reader, langConfig)
        val parser = DefaultParserFactory.create(grammar)

        var program = SyntaxProgram.empty()

        val result = lexer.peek(null)

        val token = when (result) {
            is Result.Err -> return Result.Err(result.error)
            is Result.Ok -> result.value
        }

        while (token.type != "EOF") {
            when (val result = parser.parseNextStatement(lexer, program)) {
                is Result.Err -> return Result.Err(result.error)
                is Result.Ok -> program = result.value
            }
        }

        return Result.Ok(program)
    }
}
