package printscript.evaluator

import printscript.ast.Location
import printscript.domain.GrammarRule
import printscript.domain.RepeatRule
import printscript.syntax.SyntaxNode
import printscript.util.Locations

class RepeatRuleHandler : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is RepeatRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext
    ): SyntaxNode {
        val items = collect((rule as RepeatRule).item, ctx)
        return SyntaxNode(name, children = items, location = spanOf(items, ctx))
    }

    private fun collect(item: String, ctx: ParseContext): List<SyntaxNode> {
        val items = mutableListOf<SyntaxNode>()
        while (true) {
            val next = ctx.tryEvaluate(item) ?: break
            items += next
        }
        return items
    }

    private fun spanOf(items: List<SyntaxNode>, ctx: ParseContext): Location =
        Locations.span(items, ctx.tokens.peek().location)
}
