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
        var error: Error? = null
        var isEof = false

        while (error == null && !isEof) {
            when (val peeked = lexer.peek(null)) {
                is Result.Err -> error = peeked.error
                is Result.Ok -> {
                    if (peeked.value.type == "EOF") {
                        isEof = true
                    } else {
                        when (val parsed = parser.parseNextStatement(lexer, program)) {
                            is Result.Err -> error = parsed.error
                            is Result.Ok -> program = parsed.value
                        }
                    }
                }
            }
        }

        return if (error != null) Result.Err(error) else Result.Ok(program)
    }
}
