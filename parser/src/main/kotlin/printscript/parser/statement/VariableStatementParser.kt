package printscript.parser.statement

import printscript.common.ast.*
import printscript.common.ast.Identifier
import printscript.common.domain.*
import printscript.parser.expression.ExpressionParser
import printscript.parser.token.TokenSource
import printscript.common.domain.Identifier as IdentifierToken

/**
 * let <id> : <type> = <expression> ;
 */
class VariableStatementParser : StatementParser {

    override fun getSteps(): List<Step> {
        return listOf(
            Step.Expect(Let()),
            Step.ExpectWithValue(IdentifierToken()),
            Step.Expect(Colon()),
            Step.ExpectWithValue(Type()),
            Step.Expect(Assign()),
            Step.Expr,
            Step.Expect(Semicolon())
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
            typeAnnotation = typeToken.value.get(),
            initializer = initializer,
            location = match.location
        )

        return VariableStatement(
            declaration = declaration,
            location = match.location
        )
    }
}