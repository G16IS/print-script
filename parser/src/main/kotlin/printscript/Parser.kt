package printscript

import printscript.syntax.SyntaxProgram

/**
 * Transforms a token stream into a [SyntaxProgram] one statement at a time.
 * Syntactic analysis only — no semantic validation.
 */
interface Parser {
    fun parseNextStatement(
        tokenStream: Lexer,
        program: SyntaxProgram,
    ): SyntaxProgram
}
