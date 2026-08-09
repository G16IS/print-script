package printscript.parser.statement

import printscript.common.ast.CallExpression
import printscript.common.ast.ExpressionStatement
import printscript.common.ast.Statement
import printscript.lexer.LeftParen
import printscript.lexer.Print
import printscript.lexer.RightParen
import printscript.lexer.Semicolon
import printscript.parser.expression.ExpressionParser
import printscript.parser.token.TokenSource
import printscript.parser.util.Locations

/**
 * println ( <expression> ) ;
 *
 * Represented as ExpressionStatement(CallExpression("println", ...)).
 */
class PrintStatementParser : StatementParser {
    override fun parse(tokens: TokenSource, expressions: ExpressionParser): Statement? {
        if (tokens.peek().type !is Print) return null

        val printToken = tokens.advance()
        tokens.expect({ it is LeftParen }, "Expected '(' after 'println'")

        val arg = expressions.parse(tokens)

        tokens.expect({ it is RightParen }, "Expected ')' after println argument")
        val semicolon = tokens.expect({ it is Semicolon }, "Expected ';' after println statement")

        val call = CallExpression(
            callee = "println",
            args = listOf(arg),
            location = Locations.between(printToken, semicolon)
        )

        return ExpressionStatement(
            expression = call,
            location = Locations.between(printToken, semicolon)
        )
    }
}
