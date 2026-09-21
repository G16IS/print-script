package printscript.parser.parse

import printscript.domain.GrammarRule
import printscript.domain.RepeatRule
import printscript.error.ParserError
import printscript.parser.util.Locations
import printscript.syntax.Location
import printscript.syntax.SyntaxNode

class RepeatRuleHandler : RuleHandler {
    override fun supports(rule: GrammarRule): Boolean = rule is RepeatRule

    override fun evaluate(
        name: String,
        rule: GrammarRule,
        ctx: ParseContext,
    ): ParseResult<SyntaxNode> =
        when (val result = collect((rule as RepeatRule).item, ctx)) {
            is CollectResult.Items ->
                ParseResult.Matched(
                    SyntaxNode(name, children = result.items, location = spanOf(result.items, ctx)),
                )

            is CollectResult.Failed -> ParseResult.Failed(result.error)
        }

    private fun collect(
        item: String,
        ctx: ParseContext,
    ): CollectResult {
        val items = mutableListOf<SyntaxNode>()
        while (true) {
            val before = ctx.tokens.checkpoint()
            when (val next = ctx.tryEvaluate(item)) {
                is ParseResult.Matched -> {
                    val after = ctx.tokens.checkpoint()
                    check(after != before) {
                        "Repeat of '$item' matched without consuming tokens"
                    }
                    items += next.node
                }

                ParseResult.Missing -> return CollectResult.Items(items)
                is ParseResult.Failed -> return CollectResult.Failed(next.error)
            }
        }
    }

    private fun spanOf(
        items: List<SyntaxNode>,
        ctx: ParseContext,
    ): Location = Locations.span(items, ctx.tokens.peek().location)
}

private sealed interface CollectResult {
    data class Items(
        val items: List<SyntaxNode>,
    ) : CollectResult

    data class Failed(
        val error: ParserError,
    ) : CollectResult
}
