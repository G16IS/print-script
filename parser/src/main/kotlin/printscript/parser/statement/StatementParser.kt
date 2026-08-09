package printscript.parser.statement

import printscript.common.ast.Statement
import printscript.parser.expression.ExpressionParser
import printscript.parser.token.TokenSource

/**
 * Strategy for one kind of statement. Returns null if the current token
 * is not handled by this parser (so the registry can try the next one).
 */
fun interface StatementParser {
    fun parse(tokens: TokenSource, expressions: ExpressionParser): Statement?
}
