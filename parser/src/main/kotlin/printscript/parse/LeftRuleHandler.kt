package printscript.parse

import printscript.domain.GrammarRule
import printscript.domain.LeftRule
import printscript.domain.OperatorSpec
import printscript.domain.Token
import printscript.error.ParseErrors
import printscript.syntax.SyntaxNode
import printscript.util.binary
import printscript.util.wrap

class LeftRuleHandler : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is LeftRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> {
        val left = rule as LeftRule
        return when (val first = ctx.evaluate(left.left)) {
            is ParseResult.Matched -> consumeOperators(name, left, first.node, ctx)
            ParseResult.Missing -> ParseResult.Missing
            is ParseResult.Failed -> ParseResult.Failed(first.error)
        }
    }

    private fun consumeOperators(
        name: String,
        left: LeftRule,
        first: SyntaxNode,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> {
        var acc = first
        var combined = false
        while (matchesOp(ctx.tokens.peek(), left.op)) {
            when (val extended = extend(name, acc, left, ctx)) {
                is ParseResult.Matched -> {
                    acc = extended.node
                    combined = true
                }

                is ParseResult.Failed -> return extended
                ParseResult.Missing ->
                    error("Left operand of '$name' matched without producing a node")
            }
        }
        return ParseResult.Matched(if (combined) acc else wrap(name, first))
    }

    private fun extend(
        name: String,
        acc: SyntaxNode,
        left: LeftRule,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> {
        val op = ctx.tokens.advance()
        return when (val right = ctx.evaluate(left.left)) {
            is ParseResult.Matched -> ParseResult.Matched(binary(name, acc, op, right.node))
            is ParseResult.Failed -> ParseResult.Failed(right.error)
            ParseResult.Missing ->
                ParseResult.Failed(ParseErrors.unexpectedToken(ctx.tokens.peek(), left.left))
        }
    }

    private fun matchesOp(
        token: Token,
        op: OperatorSpec,
    ): Boolean {
        val result = token.type == op.token

        return result && ((token.value.orElse(null) ?: false) in (op.values))
    }
}
