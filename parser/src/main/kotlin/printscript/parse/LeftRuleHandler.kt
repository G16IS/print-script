package printscript.parse

import printscript.domain.Token
import printscript.error.ParseErrors
import printscript.domain.GrammarRule
import printscript.domain.LeftRule
import printscript.domain.OperatorSpec
import printscript.syntax.SyntaxNode
import printscript.util.binary
import printscript.util.wrap

class LeftRuleHandler : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is LeftRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext
    ): SyntaxNode? {
        val left = rule as LeftRule
        val first = ctx.evaluate(left.left) ?: return null
        return consumeOperators(name, left, first, ctx)
    }

    private fun consumeOperators(
        name: String,
        left: LeftRule,
        first: SyntaxNode,
        ctx: ParseContext
    ): SyntaxNode {
        var acc = first
        var combined = false
        while (matchesOp(ctx.tokens.peek(), left.op)) {
            acc = extend(name, acc, left, ctx)
            combined = true
        }
        return if (combined) acc else wrap(name, first)
    }

    private fun extend(
        name: String,
        acc: SyntaxNode,
        left: LeftRule,
        ctx: ParseContext
    ): SyntaxNode {
        val op = ctx.tokens.advance()
        val right = ctx.evaluate(left.left)
            ?: throw ParseErrors.unexpectedToken(ctx.tokens.peek(), left.left)
        return binary(name, acc, op, right)
    }

    private fun matchesOp(token: Token, op: OperatorSpec): Boolean {
        if (token.type != op.token) return false
        val value = token.value.orElse(null) ?: return false
        return value in op.values
    }
}
