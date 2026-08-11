package printscript

import printscript.common.ast.Program
import printscript.Lexer

/**
 * Transforms a token stream into an AST [Program] step.
 * Performs syntactic analysis only — no semantic validation.
 */
interface Parser {
    /**
     * Parse next statement given a token stream and an existing program. Returns the updated program with the new statement added.
     *
     * @param tokenStream The token stream to provide tokens
     * @param program The existing program to add the new statement to
     * @return The updated program with the new statement added
     */
    fun parseNextStatement(tokenStream: Lexer, program: Program): Program
}
