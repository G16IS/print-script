package printscript.parse

import printscript.ast.Location
import printscript.error.ParseErrors
import printscript.parse.step.StepEvaluator
import printscript.parse.step.StepOutcome
import printscript.domain.GrammarRule
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.SeqStep
import printscript.domain.TokenStep
import printscript.syntax.SyntaxNode

class SeqRuleHandler(
    private val stepEvaluator: StepEvaluator = StepEvaluator()
) : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is SeqRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext
    ): SyntaxNode? {
        val match = runSteps((rule as SeqRule).steps, ctx) ?: return null
        return SyntaxNode(name, children = match.children, location = match.location)
    }

    private fun runSteps(steps: List<SeqStep>, ctx: ParseContext): SeqMatch? {
        val mark = ctx.tokens.checkpoint()
        val children = mutableListOf<SyntaxNode>()
        val seen = mutableListOf<Location>()
        return walkSteps(steps, ctx, mark, children, seen)
    }

    private fun walkSteps(
        steps: List<SeqStep>,
        ctx: ParseContext,
        mark: Int,
        children: MutableList<SyntaxNode>,
        seen: MutableList<Location>
    ): SeqMatch? {
        for ((index, step) in steps.withIndex()) {
            val outcome = stepEvaluator.evaluate(step, ctx)
            if (!outcome.matched) return abort(index, mark, ctx, step)
            collect(outcome, children, seen)
        }
        return SeqMatch(children, spanOf(seen, ctx))
    }

    private fun collect(
        outcome: StepOutcome,
        children: MutableList<SyntaxNode>,
        seen: MutableList<Location>
    ) {
        outcome.node?.let { children += it }
        outcome.location?.let { seen += it }
    }

    private fun abort(
        index: Int,
        mark: Int,
        ctx: ParseContext,
        step: SeqStep
    ): SeqMatch? {
        if (index == 0) {
            ctx.tokens.restore(mark)
            return null
        }
        throw ParseErrors.unexpectedToken(ctx.tokens.peek(), describe(step))
    }

    private fun spanOf(seen: List<Location>, ctx: ParseContext): Location {
        if (seen.isEmpty()) return ctx.tokens.peek().location
        return Location(seen.first().start, seen.last().end)
    }

    private fun describe(step: SeqStep): String = when (step) {
        is TokenStep -> step.type
        is RuleRefStep -> step.name
        else -> step.toString()
    }
}

private data class SeqMatch(
    val children: List<SyntaxNode>,
    val location: Location
)
