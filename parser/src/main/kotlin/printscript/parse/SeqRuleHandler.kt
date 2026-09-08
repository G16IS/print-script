package printscript.parse

import printscript.ast.Location
import printscript.domain.GrammarRule
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.SeqStep
import printscript.domain.TokenStep
import printscript.error.ParseErrors
import printscript.error.ParserError
import printscript.parse.step.StepEvaluator
import printscript.parse.step.StepOutcome
import printscript.syntax.SyntaxNode

class SeqRuleHandler(
    private val stepEvaluator: StepEvaluator = StepEvaluator(),
) : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is SeqRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> =
        when (val result = runSteps((rule as SeqRule).steps, ctx)) {
            is SeqResult.Match ->
                ParseResult.Matched(
                    SyntaxNode(name, children = result.children, location = result.location),
                )

            SeqResult.Miss -> ParseResult.Missing
            is SeqResult.Failed -> ParseResult.Failed(result.error)
        }

    private fun runSteps(
        steps: List<SeqStep>,
        ctx: ParseContext,
    ): SeqResult {
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
        seen: MutableList<Location>,
    ): SeqResult {
        var result: SeqResult? = null
        var index = 0
        while (result == null && index < steps.size) {
            val step = steps[index]
            when (val outcome = stepEvaluator.evaluate(step, ctx)) {
                is StepOutcome.Hit -> collect(outcome, children, seen)
                is StepOutcome.Failed -> result = SeqResult.Failed(outcome.error)
                is StepOutcome.Miss ->
                    result =
                        if (index == 0) {
                            ctx.tokens.restore(mark)
                            SeqResult.Miss
                        } else {
                            SeqResult.Failed(unexpectedToken(ctx, step))
                        }
            }
            index += 1
        }
        return result ?: SeqResult.Match(children, spanOf(seen, ctx))
    }

    private fun collect(
        outcome: StepOutcome.Hit,
        children: MutableList<SyntaxNode>,
        seen: MutableList<Location>,
    ) {
        outcome.node?.let { children += it }
        outcome.location?.let { seen += it }
    }

    private fun unexpectedToken(
        ctx: ParseContext,
        step: SeqStep,
    ): ParserError = ParseErrors.unexpectedToken(ctx.tokens.peek(), describe(step))

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

private sealed interface SeqResult {
    data class Match(
        val children: List<SyntaxNode>,
        val location: Location,
    ) : SeqResult

    data object Miss : SeqResult

    data class Failed(
        val error: ParserError,
    ) : SeqResult
}
