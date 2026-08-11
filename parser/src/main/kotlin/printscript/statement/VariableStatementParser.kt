package printscript.statement

import printscript.ast.Identifier
import printscript.ast.Location
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.VariableStatement
import printscript.ast.VariableType
import printscript.domain.TokenType
import printscript.expression.ExpressionParser
import printscript.token.TokenSource

/**
 * let <id> : <type> = <expression> ;
 */
class VariableStatementParser : StatementParser {

    override fun getSteps(): List<Step> {
        return listOf(
            Step.Expect(TokenType.LET),
            Step.ExpectWithValue(TokenType.IDENTIFIER),
            Step.Expect(TokenType.COLON),
            Step.ExpectWithValue(TokenType.TYPE),
            Step.Expect(TokenType.ASSIGN),
            Step.Expr,
            Step.Expect(TokenType.SEMICOLON)
        )
    }

    override fun parse(tokens: TokenSource, expressions: ExpressionParser): Statement? {
        val match = matchSteps(getSteps(), tokens, expressions) ?: return null

        val identifier = match.valuedTokens[0]
        val typeToken = match.valuedTokens[1]
        val initializer = match.expressions.single()

        val declaration = VariableDeclaration(
            id = Identifier(
                name = identifier.value.get(),
                location = Location(identifier.start, identifier.end)
            ),
            typeAnnotation = VariableType.from(typeToken.value.get()),
            initializer = initializer,
            location = match.location
        )

        return VariableStatement(
            declaration = declaration,
            location = match.location
        )
    }
}
