package printscript.parser

import printscript.Lexer
import printscript.domain.Grammar
import printscript.error.ParserError
import printscript.parser.parse.ParseResult
import printscript.parser.parse.RuleEvaluator
import printscript.parser.token.LexerTokenSource
import printscript.parser.token.TokenSource
import printscript.syntax.SyntaxNode
import printscript.util.Result

class DefaultParser(
    private val grammar: Grammar,
    private val evaluator: RuleEvaluator,
) : Parser {
    private var boundLexer: Lexer? = null
    private var source: TokenSource? = null

    override fun parseNextStatement(tokenStream: Lexer): Result<SyntaxNode, ParserError> {
        val tokens = bind(tokenStream)
        return when (val result = evaluator.evaluate(grammar.start, tokens)) {
            is ParseResult.Matched -> Result.Ok(result.node)
            ParseResult.Missing -> Result.Err(ParseErrors.unexpectedStart(tokens.peek()))
            is ParseResult.Failed -> Result.Err(result.error)
        }
    }

    private fun bind(lexer: Lexer): TokenSource {
        if (boundLexer === lexer && source != null) return source!!
        boundLexer = lexer
        source = LexerTokenSource(lexer)
        return source!!
    }
}
