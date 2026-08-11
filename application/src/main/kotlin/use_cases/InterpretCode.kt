package edu.austral.dissis.use_cases

import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.DefaultSemanticAnalyzer
import printscript.Lexer
import printscript.Parser
import printscript.SemanticResult
import printscript.ast.Program
import printscript.domain.TokenType
import printscript.infrastructure.reader.FileCodeReader
import printscript.reader.CodeReader
import java.io.File

/**
 * Lex + parse + semantic analysis of a PrintScript source file.
 * Returns the validated [Program] or throws if semantic analysis fails.
 */
fun interpretCode(filePath: String): Program {
    val codeReader: CodeReader = FileCodeReader(filePath)
    val lexer: Lexer = DefaultLexerFactory.create(codeReader)
    val parser: Parser = DefaultParserFactory.create()

    var program: Program = Program.empty()
    while (lexer.peek(null).type != TokenType.EOF) {
        program = parser.parseNextStatement(lexer, program)
    }

    return when (val result = DefaultSemanticAnalyzer().analyze(program)) {
        is SemanticResult.Success -> result.program
        is SemanticResult.Failure -> {
            val messages = result.errors.joinToString("\n") {
                "  - ${it.messageError} @ ${it.location}"
            }
            error("Semantic analysis failed:\n$messages")
        }
    }
}

/**
 * Same as [interpretCode] but loads a classpath resource (e.g. `"/examples/foo.ps"`).
 */
fun interpretCodeFromResource(
    resourceName: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): Program {
    val normalized = resourceName.removePrefix("/")
    val resourceUrl = classLoader.getResource(normalized)
        ?: error("No se encontró el recurso $resourceName en el classpath")
    return interpretCode(File(resourceUrl.toURI()).absolutePath)
}
