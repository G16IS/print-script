package printscript.usecases

import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.edition.LanguageKit
import printscript.error.Error
import printscript.reader.CodeReader
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result

internal object ParseProgram {
    private const val END_TOKEN = "EOF"

    fun parseStatements(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        kit: LanguageKit,
    ): Sequence<Result<SyntaxNode, Error>> =
        sequence {
            val lexer = DefaultLexerFactory.create(reader, langConfig)
            val parser = DefaultParserFactory.create(grammar, kit.parserHandlers)

            var running = true
            while (running) {
                when (val peeked = lexer.peek(null)) {
                    is Result.Err -> {
                        yield(Result.Err(peeked.error))
                        running = false
                    }
                    is Result.Ok -> {
                        if (peeked.value.type == END_TOKEN) {
                            running = false
                        } else {
                            when (val parsed = parser.parseNextStatement(lexer)) {
                                is Result.Err -> {
                                    yield(Result.Err(parsed.error))
                                    running = false
                                }
                                is Result.Ok -> yield(Result.Ok(parsed.value))
                            }
                        }
                    }
                }
            }
        }

    fun parse(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
        kit: LanguageKit,
    ): Result<SyntaxProgram, Error> {
        val builder = SyntaxProgram.builder()
        for (statementResult in parseStatements(langConfig, grammar, reader, kit)) {
            when (statementResult) {
                is Result.Err -> return Result.Err(statementResult.error)
                is Result.Ok -> builder.add(statementResult.value)
            }
        }
        return Result.Ok(builder.build())
    }
}
