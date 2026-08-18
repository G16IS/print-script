package printscript.support

import printscript.evaluator.RuleEvaluator
import printscript.evaluator.RuleHandlers
import printscript.domain.Grammar
import printscript.syntax.SyntaxNode
import printscript.token.LexerTokenSource
import printscript.token.TokenSource
import printscript.domain.Token

fun evaluator(grammar: Grammar): RuleEvaluator =
    RuleEvaluator(grammar, RuleHandlers.defaults())

fun source(vararg tokens: Token): TokenSource =
    LexerTokenSource(MockLexer(tokens.toList()))

fun parse(grammar: Grammar, vararg tokens: Token): SyntaxNode =
    evaluator(grammar).evaluate(grammar.start, source(*tokens))
        ?: error("Expected a match for start rule '${grammar.start}'")

fun parseOrNull(grammar: Grammar, vararg tokens: Token): SyntaxNode? =
    evaluator(grammar).evaluate(grammar.start, source(*tokens))
