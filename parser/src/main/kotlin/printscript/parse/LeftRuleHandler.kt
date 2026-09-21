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
        val leftRule = rule as LeftRule
        val first = ctx.evaluate(leftRule.left)
        if (first !is ParseResult.Matched) return first
        return foldOperators(name, leftRule, first.node, ctx)
    }

    private fun foldOperators(
        name: String,
        spec: LeftRule,
        first: SyntaxNode,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> {
        var acc = first
        while (matchesOp(ctx.tokens.peek(), spec.op)) {
            val op = ctx.tokens.advance()
            when (val right = ctx.evaluate(spec.left)) {
                is ParseResult.Matched -> acc = binary(name, acc, op, right.node)
                is ParseResult.Failed -> return right
                ParseResult.Missing ->
                    return ParseResult.Failed(
                        ParseErrors.unexpectedToken(ctx.tokens.peek(), spec.left),
                    )
            }
        }
        return ParseResult.Matched(if (acc === first) wrap(name, first) else acc)
    }

    private fun matchesOp(
        token: Token,
        op: OperatorSpec,
    ): Boolean {
        val result = token.type == op.token

        return result && ((token.value.orElse(null) ?: false) in (op.values))
    }
}
