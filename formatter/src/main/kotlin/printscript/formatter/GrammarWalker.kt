package printscript.formatter

import printscript.domain.Grammar
import printscript.domain.OptionalRule
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.SeqStep
import printscript.domain.TokenLexemes
import printscript.domain.TokenStep
import printscript.error.FormatError
import printscript.error.UnrecognizedFormatNode
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

/**
 * Drives formatter emission from grammar `SeqRule`s and `OptionalRule`s.
 *
 * Children are consumed in order (captured tokens and rule-refs). Uncaptured
 * tokens are synthetic and do not advance the cursor, so two rule-refs with
 * the same name (then/else) emit as distinct children.
 *
 * `OptionalRule`: 0 children emits nothing; 1 child is walked; more is
 * unrecognized. Other non-SeqRule nodes (OrRule, LeftRule, AtomRule,
 * RepeatRule) return null and fall through to the generic emitChildren path.
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
            is OptionalRule -> emitOptional(node, state, failFast, walk)
            else -> null
        }
    }

    private fun emitOptional(
        node: SyntaxNode,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> =
        when (node.children.size) {
            0 -> Result.Ok(state)
            1 -> walk.emit(node.children.single(), node.name, state, failFast)
            else -> failWalk(UnrecognizedFormatNode(node.name, node.location), state, failFast)
        }

    private fun emitSeq(
        node: SyntaxNode,
        rule: SeqRule,
        state: WalkState,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<WalkState, FormatError> {
        val start: Result<SeqWalk, FormatError> = Result.Ok(SeqWalk(state, 0))

        return rule.steps
            .fold(start) { acc, step ->
                acc.flatMap { current ->
                    emitStep(node, step, current, failFast, walk)
                }
            }.flatMap { done ->
                if (done.childIndex == node.children.size) {
                    Result.Ok(done.state)
                } else {
                    failWalk(
                        UnrecognizedFormatNode(node.name, node.location),
                        done.state,
                        failFast,
                    )
                }
            }
    }

    private fun emitStep(
        node: SyntaxNode,
        step: SeqStep,
        current: SeqWalk,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<SeqWalk, FormatError> =
        when (step) {
            is TokenStep -> emitTokenStep(node, step, current, failFast, walk)
            is RuleRefStep -> consumeChild(node, current, failFast, walk)
            else ->
                failWalk(
                    UnrecognizedFormatNode("${node.name}/unknown-step", node.location),
                    current.state,
                    failFast,
                ).map { SeqWalk(it, current.childIndex) }
        }

    private fun emitTokenStep(
        node: SyntaxNode,
        step: TokenStep,
        current: SeqWalk,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<SeqWalk, FormatError> =
        if (step.capture) {
            consumeChild(node, current, failFast, walk)
        } else {
            emitSyntheticToken(node, step.type, current, failFast, walk)
        }

    private fun consumeChild(
        node: SyntaxNode,
        current: SeqWalk,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<SeqWalk, FormatError> {
        val child =
            node.children.getOrNull(current.childIndex)
                ?: return failWalk(
                    UnrecognizedFormatNode(node.name, node.location),
                    current.state,
                    failFast,
                ).map { SeqWalk(it, current.childIndex) }

        return walk.emit(child, node.name, current.state, failFast).map { emitted ->
            SeqWalk(emitted, current.childIndex + 1)
        }
    }

    private fun emitSyntheticToken(
        node: SyntaxNode,
        type: String,
        current: SeqWalk,
        failFast: Boolean,
        walk: NodeWalk,
    ): Result<SeqWalk, FormatError> {
        val lexeme =
            lexemes.lexemeFor(type)
                ?: return failWalk(
                    UnrecognizedFormatNode("${node.name}/$type", node.location),
                    current.state,
                    failFast,
                ).map { SeqWalk(it, current.childIndex) }

        return walk.emitSynthetic(type, lexeme, node.name, current.state, failFast).map { emitted ->
            SeqWalk(emitted, current.childIndex)
        }
    }
}

private data class SeqWalk(
    val state: WalkState,
    val childIndex: Int,
)
