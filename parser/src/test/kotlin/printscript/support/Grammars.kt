package printscript.support

import printscript.grammar.AtomRule
import printscript.grammar.Grammar
import printscript.grammar.GrammarRule
import printscript.grammar.LeftRule
import printscript.grammar.OperatorSpec
import printscript.grammar.OrRule
import printscript.grammar.RepeatRule
import printscript.grammar.RuleRefStep
import printscript.grammar.SeqRule
import printscript.grammar.SeqStep
import printscript.grammar.TokenStep

fun grammar(start: String, vararg rules: Pair<String, GrammarRule>): Grammar =
    Grammar(start, rules.toMap())

fun atom(token: String) = AtomRule(token)

fun or(vararg alternatives: String) = OrRule(alternatives.toList())

fun seq(vararg steps: SeqStep) = SeqRule(steps.toList())

fun expect(type: String) = TokenStep(type, capture = false)

fun capture(type: String) = TokenStep(type, capture = true)

fun ref(name: String) = RuleRefStep(name)

fun repeat(item: String) = RepeatRule(item)

fun left(operand: String, token: String, vararg values: String) =
    LeftRule(operand, OperatorSpec(token, values.toList()))
