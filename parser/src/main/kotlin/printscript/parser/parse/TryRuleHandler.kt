package printscript.parser.parse

import printscript.domain.GrammarRule
import printscript.domain.TryRule
import printscript.syntax.SyntaxNode

class TryRuleHandler : RuleHandler {
    override fun supports(rule: GrammarRule) = rule is TryRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> {
        val mark = ctx.tokens.checkpoint()
        return when (val inner = ctx.evaluate((rule as TryRule).item)) {
            is ParseResult.Matched ->
                ParseResult.Matched(
                    SyntaxNode(name, children = inner.node.children, location = inner.node.location),
                )
            ParseResult.Missing -> inner
            is ParseResult.Failed -> {
                ctx.tokens.restore(mark)
                ParseResult.Missing
            }
        }
    }
}
