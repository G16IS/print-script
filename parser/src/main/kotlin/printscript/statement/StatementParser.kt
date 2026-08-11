package printscript.statement

import printscript.ast.Statement
import printscript.expression.ExpressionParser
import printscript.token.TokenSource

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