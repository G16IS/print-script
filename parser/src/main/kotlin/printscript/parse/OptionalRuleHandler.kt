package printscript.parse

import printscript.domain.GrammarRule
import printscript.domain.OptionalRule
import printscript.syntax.SyntaxNode

class OptionalRuleHandler : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is OptionalRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> {
        val item = (rule as OptionalRule).item
        return when (val inner = ctx.tryEvaluate(item)) {
            is ParseResult.Matched -> ParseResult.Matched(
                SyntaxNode(name, children = listOf(inner.node), location = inner.node.location),
            )

            ParseResult.Missing -> ParseResult.Matched(
                SyntaxNode(name, children = emptyList(), location = ctx.tokens.peek().location),
            )

            is ParseResult.Failed -> inner
        }
    }
}
