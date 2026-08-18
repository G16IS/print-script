package printscript.evaluator

import printscript.grammar.GrammarRule
import printscript.grammar.OrRule
import printscript.syntax.SyntaxNode

class OrRuleHandler : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is OrRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext
    ): SyntaxNode? = firstMatch((rule as OrRule).alternatives, ctx)

    private fun firstMatch(alternatives: List<String>, ctx: ParseContext): SyntaxNode? {
        for (alternative in alternatives) {
            val node = ctx.tryEvaluate(alternative)
            if (node != null) return node
        }
        return null
    }
}
