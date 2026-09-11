package printscript.parse.step

import printscript.domain.RuleRefStep
import printscript.domain.SeqStep
import printscript.domain.TokenStep
import printscript.parse.ParseContext
import printscript.parse.ParseResult
import printscript.util.tokenLeaf

class StepEvaluator {
    fun evaluate(
        step: SeqStep,
        ctx: ParseContext,
    ): StepOutcome =
        when (step) {
            is TokenStep -> consume(step, ctx)
            is RuleRefStep -> reference(step, ctx)
            else -> error("Unknown seq step: $step")
        }

    private fun consume(
        step: TokenStep,
        ctx: ParseContext,
    ): StepOutcome {
        val token = ctx.tokens.peek()
        if (token.type != step.type) return StepOutcome.Miss
        ctx.tokens.advance()
        return if (step.capture) {
            StepOutcome.Hit(tokenLeaf(token))
        } else {
            StepOutcome.Hit(location = token.location)
        }
    }

    private fun reference(
        step: RuleRefStep,
        ctx: ParseContext,
    ): StepOutcome =
        when (val result = ctx.evaluate(step.name)) {
            is ParseResult.Matched -> StepOutcome.Hit(result.node)
            ParseResult.Missing -> StepOutcome.Miss
            is ParseResult.Failed -> StepOutcome.Failed(result.error)
        }
}
