package usecases

import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.infrastructure.reader.FileCodeReader
import printscript.syntax.SyntaxProgram

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

        while (lexer.peek(null).type != "EOF") {
            program = parser.parseNextStatement(lexer, program)
        }

        return program
    }
}
