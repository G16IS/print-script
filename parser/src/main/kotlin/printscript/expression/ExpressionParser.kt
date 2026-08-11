package printscript.expression

import printscript.ast.Expression
import printscript.token.TokenSource

/**
 * Parses expression productions. Implementations own operator precedence.
 */
interface ExpressionParser {
    fun parse(tokens: TokenSource): Expression
}
