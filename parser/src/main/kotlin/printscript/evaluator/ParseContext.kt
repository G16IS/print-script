package printscript.evaluator

import printscript.grammar.Grammar
import printscript.syntax.SyntaxNode
import printscript.token.TokenSource

class ParseContext(
    val grammar: Grammar,
    val tokens: TokenSource,
    private val evaluateRule: (String) -> SyntaxNode?
) {
    fun evaluate(ruleName: String): SyntaxNode? = evaluateRule(ruleName)

    fun tryEvaluate(ruleName: String): SyntaxNode? {
        val mark = tokens.checkpoint()
        val node = evaluate(ruleName)
        if (node == null) tokens.restore(mark)
        return node
    }
}
