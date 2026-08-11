package printscript.statement

import printscript.common.ast.CallExpression
import printscript.common.ast.ExpressionStatement
import printscript.common.ast.Statement
import printscript.common.domain.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.token.TokenSource

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
