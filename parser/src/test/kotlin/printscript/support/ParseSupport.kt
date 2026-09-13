package printscript.support

import printscript.domain.Grammar
import printscript.domain.Token
import printscript.parse.ParseResult
import printscript.parse.RuleEvaluator
import printscript.parse.RuleHandlers
import printscript.syntax.SyntaxNode
import printscript.token.LexerTokenSource
import printscript.token.TokenSource

fun evaluator(grammar: Grammar): RuleEvaluator = RuleEvaluator(grammar, RuleHandlers.defaults())

fun source(vararg tokens: Token): TokenSource = LexerTokenSource(MockLexer(tokens.toList()))

fun parse(
    grammar: Grammar,
    vararg tokens: Token,
): SyntaxNode =
    when (val result = evaluator(grammar).evaluate(grammar.start, source(*tokens))) {
        is ParseResult.Matched -> result.node
        is ParseResult.Failed -> error("Failed to match start rule '${grammar.start}': ${result.error.message}")
        ParseResult.Missing -> error("Expected a match for start rule '${grammar.start}'")
    }

fun parseOrNull(
    grammar: Grammar,
    vararg tokens: Token,
): SyntaxNode? =
    when (val result = evaluator(grammar).evaluate(grammar.start, source(*tokens))) {
        is ParseResult.Matched -> result.node
        else -> null
    }
