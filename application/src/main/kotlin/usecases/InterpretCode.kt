package usecases

import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.Lexer
import printscript.Parser
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.infrastructure.reader.FileCodeReader
import printscript.syntax.SyntaxProgram
import printscript.typechecker.DefaultTypeCheckerFactory
import printscript.typechecker.TypeError

object InterpretCode {
    fun interpretCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        path: String,
    ): SyntaxProgram {
        val codeReader = FileCodeReader(path)

        val lexer: Lexer = DefaultLexerFactory.create(codeReader, langConfig)
        val parser: Parser = DefaultParserFactory.create(grammar)

        var program: SyntaxProgram = SyntaxProgram.empty()

        while (lexer.peek(null).type != "EOF") {
            program = parser.parseNextStatement(lexer, program)
        }

        val report = DefaultTypeCheckerFactory.create(typeSystem).check(program)

        if (!report.isOk) {
            failTypeCheck(report.errors)
        }

        return program
    }

    private fun failTypeCheck(errors: List<TypeError>): Nothing {
        val messages =
            errors.joinToString("\n") { typeError ->
                val position = typeError.location.start
                "  - ${typeError.message} @ ${position.line}:${position.col}"
            }
        error("El chequeo de tipos falló:\n$messages")
    }
}
