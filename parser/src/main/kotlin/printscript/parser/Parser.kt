package printscript.parser

import printscript.Lexer
import printscript.domain.Grammar
import printscript.error.ParserError
import printscript.parser.parse.RuleHandler
import printscript.syntax.SyntaxNode
import printscript.util.Result

/**
 * Transforms a token stream into [SyntaxNode]s one statement at a time.
 * Syntactic analysis only — no semantic validation.
 */
interface Parser {
    companion object {
        fun create(
            grammar: Grammar,
            handlers: List<RuleHandler>,
        ) = DefaultParserFactory.create(grammar, handlers)
    }

    fun parseNextStatement(tokenStream: Lexer): Result<SyntaxNode, ParserError>
}
