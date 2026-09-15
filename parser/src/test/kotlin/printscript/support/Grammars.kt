package printscript.support

import printscript.domain.AtomRule
import printscript.domain.Grammar
import printscript.domain.GrammarRule
import printscript.domain.LeftRule
import printscript.domain.OperatorSpec
import printscript.domain.OptionalRule
import printscript.domain.OrRule
import printscript.domain.RepeatRule
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.SeqStep
import printscript.domain.TokenStep

fun grammar(
    start: String,
    vararg rules: Pair<String, GrammarRule>,
): Grammar = Grammar(start, rules.toMap())

fun atom(token: String) = AtomRule(token)

fun or(vararg alternatives: String) = OrRule(alternatives.toList())

fun seq(vararg steps: SeqStep) = SeqRule(steps.toList())

fun expect(type: String) = TokenStep(type, capture = false)

fun capture(type: String) = TokenStep(type, capture = true)

fun ref(name: String) = RuleRefStep(name)

fun repeat(item: String) = RepeatRule(item)

fun optional(item: String) = OptionalRule(item)

fun left(
    operand: String,
    token: String,
    vararg values: String,
) = LeftRule(operand, OperatorSpec(token, values.toList()))
