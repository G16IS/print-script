package usecases

import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.reader.CodeReader
import printscript.syntax.SyntaxProgram

internal object ParseProgram {
    fun parse(
        langConfig: LanguageConfig,
        grammar: Grammar,
        reader: CodeReader,
    ): SyntaxProgram {
        val lexer = DefaultLexerFactory.create(reader, langConfig)
        val parser = DefaultParserFactory.create(grammar)

        var program = SyntaxProgram.empty()

        while (lexer.peek(null).type != "EOF") {
            program = parser.parseNextStatement(lexer, program)
        }

        return program
    }
}
