package printscript

import printscript.domain.Grammar
import printscript.error.ParseErrors
import printscript.error.ParserError
import printscript.parse.ParseResult
import printscript.parse.RuleEvaluator
import printscript.syntax.SyntaxNode
import printscript.token.LexerTokenSource
import printscript.token.TokenSource
import printscript.util.Result

class DefaultParser(
    private val grammar: Grammar,
    private val evaluator: RuleEvaluator,
) : Parser {
    private var boundLexer: Lexer? = null
    private var source: TokenSource? = null

    internal fun retainedTokenCount(): Int = (source as? LexerTokenSource)?.retainedCount ?: 0

    override fun parseNextStatement(tokenStream: Lexer): Result<SyntaxNode, ParserError> {
        val tokens = bind(tokenStream)
        return try {
            when (val result = evaluator.evaluate(grammar.start, tokens)) {
                is ParseResult.Matched -> Result.Ok(result.node)
                ParseResult.Missing -> Result.Err(ParseErrors.unexpectedStart(tokens.peek()))
                is ParseResult.Failed -> Result.Err(result.error)
            }
        } finally {
            tokens.releaseConsumed()
        }
    }

    private fun bind(lexer: Lexer): TokenSource {
        if (boundLexer === lexer && source != null) return source!!
        boundLexer = lexer
        source = LexerTokenSource(lexer)
        return source!!
    }
}
