package printscript.parse

import printscript.domain.AtomRule
import printscript.domain.GrammarRule
import printscript.syntax.SyntaxNode

class AtomRuleHandler : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is AtomRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext,
    ): SyntaxNode? = match(name, rule as AtomRule, ctx)

    private fun match(
        name: String,
        atom: AtomRule,
        ctx: ParseContext,
    ): SyntaxNode? {
        val token = ctx.tokens.peek()
        if (token.type != atom.token) return null
        ctx.tokens.advance()
        return SyntaxNode(name, token = token, location = token.location)
    }
}
