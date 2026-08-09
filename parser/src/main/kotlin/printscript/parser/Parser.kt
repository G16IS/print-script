package printscript.parser

import printscript.common.ast.Program
import printscript.lexer.Token

/**
 * Transforms a token stream into an AST [Program].
 * Performs syntactic analysis only — no semantic validation.
 */
interface Parser {
    fun parse(tokens: List<Token>): Program
}
