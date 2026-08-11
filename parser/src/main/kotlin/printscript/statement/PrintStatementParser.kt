package printscript.statement

import printscript.ast.CallExpression
import printscript.ast.ExpressionStatement
import printscript.ast.Statement
import printscript.domain.TokenType
import printscript.expression.ExpressionParser
import printscript.token.TokenSource

/**
 * println ( <expression> ) ;
 *
 * Represented as ExpressionStatement(CallExpression("println", ...)).
 */
class PrintStatementParser : StatementParser {
    override fun getSteps(): List<Step> {
        return listOf(
            Step.Expect(TokenType.CALL),
            Step.Expect(TokenType.LEFT_PAREN),
            Step.Expr,
            Step.Expect(TokenType.RIGHT_PAREN),
            Step.Expect(TokenType.SEMICOLON)
        )
    }

    override fun parse(tokens: TokenSource, expressions: ExpressionParser): Statement? {
        val match = matchSteps(getSteps(), tokens, expressions) ?: return null

        val call = CallExpression(
            callee = "println",
            args = listOf(match.expressions.single()),
            location = match.location
        )

        return ExpressionStatement(call, match.location)
    }
}
