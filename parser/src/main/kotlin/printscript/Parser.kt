package printscript

import printscript.error.ParserError
import printscript.syntax.SyntaxProgram
import printscript.util.Result

/**
 * Transforms a token stream into a [SyntaxProgram] one statement at a time.
 * Syntactic analysis only — no semantic validation.
 */
interface Parser {
    fun parseNextStatement(
        tokenStream: Lexer,
        program: SyntaxProgram,
    ): Result<SyntaxProgram, ParserError>
}
