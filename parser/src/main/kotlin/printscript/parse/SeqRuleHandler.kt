package printscript.parse

import printscript.domain.GrammarRule
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.SeqStep
import printscript.domain.TokenStep
import printscript.error.ParseErrors
import printscript.parse.step.StepEvaluator
import printscript.parse.step.StepOutcome
import printscript.syntax.Location
import printscript.syntax.SyntaxNode

class SeqRuleHandler(
    private val stepEvaluator: StepEvaluator = StepEvaluator(),
) : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is SeqRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> = runSteps(name, (rule as SeqRule).steps, ctx)

    private fun runSteps(
        name: String,
        steps: List<SeqStep>,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> {
        val start = ctx.tokens.checkpoint()
        val children = mutableListOf<SyntaxNode>()
        val spans = mutableListOf<Location>()

        for ((index, step) in steps.withIndex()) {
            when (val outcome = stepEvaluator.evaluate(step, ctx)) {
                is StepOutcome.Hit -> {
                    outcome.node?.let { children += it }
                    outcome.location?.let { spans += it }
                }

                is StepOutcome.Failed -> return ParseResult.Failed(outcome.error)

                StepOutcome.Miss -> {
                    if (index > 0) {
                        return ParseResult.Failed(
                            ParseErrors.unexpectedToken(ctx.tokens.peek(), describe(step)),
                        )
                    }
                    ctx.tokens.restore(start)
                    return ParseResult.Missing
                }
            }
        }
        return ParseResult.Matched(SyntaxNode(name, children = children, location = spanOf(spans, ctx)))
    }

    private fun spanOf(
        seen: List<Location>,
        ctx: ParseContext,
    ): Location {
        if (seen.isEmpty()) return ctx.tokens.peek().location
        return Location(seen.first().start, seen.last().end)
    }

    private fun describe(step: SeqStep): String =
        when (step) {
            is TokenStep -> step.type
            is RuleRefStep -> step.name
            else -> step.toString()
        }
}
