package printscript.parse

import printscript.domain.GrammarRule
import printscript.syntax.SyntaxNode

interface RuleHandler {
    fun supports(rule: GrammarRule): Boolean

    fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext,
    ): SyntaxNode?
}
