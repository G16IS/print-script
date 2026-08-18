package printscript.evaluator

import printscript.grammar.GrammarRule
import printscript.syntax.SyntaxNode

interface RuleHandler {
    fun supports(rule: GrammarRule): Boolean

    fun evaluate(name: String, rule: GrammarRule, ctx: ParseContext): SyntaxNode?
}
