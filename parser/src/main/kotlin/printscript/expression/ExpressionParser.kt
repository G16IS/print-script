package printscript.expression

import printscript.common.ast.Expression
import printscript.parser.token.TokenSource

/**
 * Parses expression productions. Implementations own operator precedence.
 */
interface ExpressionParser {
    fun parse(tokens: TokenSource): Expression
}
