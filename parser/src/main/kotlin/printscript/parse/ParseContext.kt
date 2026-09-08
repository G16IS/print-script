package printscript.parse

import printscript.domain.Grammar
import printscript.syntax.SyntaxNode
import printscript.token.TokenSource

class ParseContext(
    val grammar: Grammar,
    val tokens: TokenSource,
    private val evaluateRule: (String) -> ParseResult<SyntaxNode>,
) {
    fun evaluate(ruleName: String): ParseResult<SyntaxNode> = evaluateRule(ruleName)

    fun tryEvaluate(ruleName: String): ParseResult<SyntaxNode> {
        val mark = tokens.checkpoint()
        val result = evaluate(ruleName)
        if (result is ParseResult.Missing) tokens.restore(mark)
        return result
    }
}
