package printscript.parse.step

import printscript.domain.Token
import printscript.parse.ParseContext
import printscript.domain.RuleRefStep
import printscript.domain.SeqStep
import printscript.domain.TokenStep
import printscript.util.tokenLeaf

class StepEvaluator {
    fun evaluate(step: SeqStep, ctx: ParseContext): StepOutcome =
        when (step) {
            is TokenStep -> consume(step, ctx)
            is RuleRefStep -> reference(step, ctx)
            else -> error("Unknown seq step: $step")
        }

    private fun consume(step: TokenStep, ctx: ParseContext): StepOutcome {
        val token = ctx.tokens.peek()
        if (token.type != step.type) return StepOutcome.miss()
        ctx.tokens.advance()
        return keepIfCaptured(step, token)
    }

    private fun keepIfCaptured(step: TokenStep, token: Token): StepOutcome {
        if (!step.capture) return StepOutcome.hit(location = token.location)
        return StepOutcome.hit(tokenLeaf(token))
    }

    private fun reference(step: RuleRefStep, ctx: ParseContext): StepOutcome {
        val node = ctx.evaluate(step.name) ?: return StepOutcome.miss()
        return StepOutcome.hit(node)
    }
}
