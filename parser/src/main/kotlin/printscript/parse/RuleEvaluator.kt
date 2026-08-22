package printscript.parse

import printscript.domain.Grammar
import printscript.domain.GrammarRule
import printscript.syntax.SyntaxNode
import printscript.token.TokenSource

class RuleEvaluator(
    private val grammar: Grammar,
    private val handlers: List<RuleHandler>,
) {
    fun evaluate(
        ruleName: String,
        tokens: TokenSource,
    ): SyntaxNode? {
        val rule = grammar.rule(ruleName)
        return handlerFor(rule).evaluate(ruleName, rule, context(tokens))
    }

    private fun context(tokens: TokenSource): ParseContext =
        ParseContext(grammar, tokens) { name -> evaluate(name, tokens) }

    private fun handlerFor(rule: GrammarRule): RuleHandler =
        handlers.firstOrNull { it.supports(rule) }
            ?: error("No handler for ${rule::class.simpleName}")
}
