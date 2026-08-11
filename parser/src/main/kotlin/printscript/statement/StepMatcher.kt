package printscript.statement

import printscript.ast.Expression
import printscript.ast.Location
import printscript.domain.Token
import printscript.domain.TokenType
import printscript.reader.CharPosition
import printscript.expression.ExpressionParser
import printscript.token.TokenSource

// ─────────────────────────────────────────────
// Modelos
// ─────────────────────────────────────────────

sealed interface Step {
    data class Expect(val type: TokenType) : Step
    data class ExpectWithValue(val type: TokenType) : Step
    object Expr : Step
}

data class MatchResult(
    val location: Location,
    val expressions: List<Expression>,
    val valuedTokens: List<Token>
)

// ─────────────────────────────────────────────
// Funciones puras
// ─────────────────────────────────────────────

private fun expectToken(tokens: TokenSource, type: TokenType): Token? {
    val current = tokens.peek()
    if (current.type != type) return null
    return tokens.advance()
}

fun matchSteps(
    steps: List<Step>,
    tokens: TokenSource,
    expressions: ExpressionParser
): MatchResult? {
    var start: CharPosition? = null
    var end: CharPosition? = null

    val parsedExpressions = mutableListOf<Expression>()
    val valuedTokens = mutableListOf<Token>()

    for ((index, step) in steps.withIndex()) {
        val isFirst = index == 0
        val isLast = index == steps.lastIndex

        when (step) {
            is Step.Expect -> {
                val token = expectToken(tokens, step.type) ?: return null
                if (isFirst) start = token.start
                if (isLast) end = token.end
            }

            is Step.ExpectWithValue -> {
                val token = expectToken(tokens, step.type) ?: return null
                valuedTokens += token
                if (isFirst) start = token.start
                if (isLast) end = token.end
            }

            is Step.Expr -> {
                val expr = expressions.parse(tokens)
                parsedExpressions += expr
            }
        }
    }

    return MatchResult(
        location = Location(start!!, end!!),
        expressions = parsedExpressions,
        valuedTokens = valuedTokens
    )
}
