package printscript.parser.statement

import printscript.common.ast.Statement
import printscript.parser.expression.ExpressionParser
import printscript.parser.token.TokenSource

/**
 * Strategy for one kind of statement. Returns null if the current token
 * is not handled by this parser (so the registry can try the next one).
 */
interface StatementParser {
    /**
     * Returns the steps that this parser expects to find in order to parse a statement.
     */
    fun getSteps(): List<Step>

    /**
     * Try to parse, return null if not able to parse.
     */
    fun parse(tokens: TokenSource, expressions: ExpressionParser): Statement?
}