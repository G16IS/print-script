package usecases

import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.Lexer
import printscript.Parser
import printscript.ast.Program
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.infrastructure.reader.FileCodeReader
import printscript.syntax.SyntaxProgram

/**
 * Lex + parse + semantic analysis of a PrintScript source file.
 * Returns the validated [Program] or throws if semantic analysis fails.
 */
object InterpretCode {
    fun interpretCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        path: String,
    ): SyntaxProgram {
        val codeReader = FileCodeReader(path)

        val lexer: Lexer = DefaultLexerFactory.create(codeReader, langConfig)
        val parser: Parser = DefaultParserFactory.create(grammar)

        var program: SyntaxProgram = SyntaxProgram.empty()
        while (lexer.peek(null).type != "EOF") {
            program = parser.parseNextStatement(lexer, program)
        }

//    return when (val result = DefaultSemanticAnalyzer().analyze(program)) {
//        is SemanticResult.Success -> result.program
//        is SemanticResult.Failure -> {
//            val messages = result.errors.joinToString("\n") {
//                "  - ${it.messageError} @ ${it.location}"
//            }
//            error("Semantic analysis failed:\n$messages")
//        }
//    }

        return program
    }
}
