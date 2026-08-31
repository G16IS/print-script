package printscript.formatter

import printscript.domain.Grammar
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.SeqStep
import printscript.domain.TokenLexemes
import printscript.domain.TokenStep
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

/**
 * Drives formatter emission from grammar `SeqRule`s.
 *
 * For each SyntaxNode whose grammar rule is a SeqRule,
 * walks the steps:
 * - TokenStep(capture=true)  → emit the captured child node
 * - TokenStep(capture=false) → emit a synthetic token (lexeme from TokenLexemes)
 * - RuleRefStep              → emit the referenced child node
 *
 * Returns null for non-SeqRule nodes (OrRule, LeftRule, AtomRule, RepeatRule),
 * which fall through to the generic emitChildren path.
 */
internal class GrammarWalker(
    private val grammar: Grammar,
    private val lexemes: TokenLexemes,
) {
    fun emit(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError>? {
        val rule = grammar.rules[node.name] ?: return null
        return when (rule) {
            is SeqRule -> emitSeq(node, rule, state, failFast, walk)
            else -> null
        }
    }

    private fun emitSeq(
        node: SyntaxNode,
        rule: SeqRule,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> {
        val start: Result<WalkState, FormatError> = Result.Ok(state)

        return rule.steps.fold(start) { acc, step ->
            acc.flatMap { current ->
                emitStep(node, step, current, failFast, walk)
            }
        }
    }

    private fun emitStep(
        node: SyntaxNode,
        step: SeqStep,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> =
        when (step) {
            is TokenStep -> emitTokenStep(node, step, state, failFast, walk)
            is RuleRefStep -> emitRuleRef(node, step, state, failFast, walk)
            else -> Result.Err(UnrecognizedNode("${node.name}/unknown-step", node.location))
        }

    private fun emitTokenStep(
        node: SyntaxNode,
        step: TokenStep,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> =
        if (step.capture) {
            emitCapturedToken(node, step.type, state, failFast, walk)
        } else {
            emitSyntheticToken(node, step.type, state, failFast, walk)
        }

    private fun emitCapturedToken(
        node: SyntaxNode,
        type: String,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> {
        val child =
            node.childOrNull(type)
                ?: return Result.Err(UnrecognizedNode(node.name, node.location))
        return walk.emit(child, node.name, state, failFast)
    }

    private fun emitSyntheticToken(
        node: SyntaxNode,
        type: String,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> {
        val lexeme =
            lexemes.lexemeFor(type)
                ?: return Result.Err(UnrecognizedNode("${node.name}/$type", node.location))
        return walk.emitSynthetic(type, lexeme, node.name, state, failFast)
    }

    private fun emitRuleRef(
        node: SyntaxNode,
        step: RuleRefStep,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> {
        val child =
            node.childOrNull(step.name)
                ?: return Result.Err(UnrecognizedNode(node.name, node.location))
        return walk.emit(child, node.name, state, failFast)
    }
}
