package printscript.parser.statement

import printscript.common.ast.CallExpression
import printscript.common.ast.ExpressionStatement
import printscript.common.ast.Statement
import printscript.common.domain.Call
import printscript.common.domain.LeftParen
import printscript.common.domain.RightParen
import printscript.common.domain.Semicolon
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
            Step.Expect(Call()),
            Step.Expect(LeftParen()),
            Step.Expr,
            Step.Expect(RightParen()),
            Step.Expect(Semicolon())
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