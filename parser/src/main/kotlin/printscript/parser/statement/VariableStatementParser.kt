package printscript.parser.statement

import printscript.common.ast.Identifier
import printscript.common.ast.Statement
import printscript.common.ast.VariableDeclaration
import printscript.common.ast.VariableStatement
import printscript.lexer.Assign
import printscript.lexer.Colon
import printscript.lexer.Identifier as IdentifierToken
import printscript.lexer.Let
import printscript.lexer.Semicolon
import printscript.lexer.Type
import printscript.parser.error.ParseException
import printscript.parser.expression.ExpressionParser
import printscript.parser.token.TokenSource
import printscript.parser.util.Locations

/**
 * let <id> : <type> = <expression> ;
 */
class VariableStatementParser : StatementParser {
    override fun parse(tokens: TokenSource, expressions: ExpressionParser): Statement? {
        if (tokens.peek().type !is Let) return null

        val letToken = tokens.advance()

        val idToken = tokens.expect({ it is IdentifierToken }, "Expected identifier after 'let'")
        val name = idToken.value.orElseThrow {
            ParseException("Identifier missing value", Locations.of(idToken))
        }
        val identifier = Identifier(name, Locations.of(idToken))

        tokens.expect({ it is Colon }, "Expected ':' after identifier in variable declaration")

        val typeToken = tokens.expect({ it is Type }, "Expected type after ':' in variable declaration")
        val typeAnnotation = typeToken.value.orElseThrow {
            ParseException("Type token missing value", Locations.of(typeToken))
        }

        tokens.expect({ it is Assign }, "Expected '=' after type in variable declaration")

        val initializer = expressions.parse(tokens)

        val semicolon = tokens.expect({ it is Semicolon }, "Expected ';' after variable declaration")

        val declaration = VariableDeclaration(
            id = identifier,
            typeAnnotation = typeAnnotation,
            initializer = initializer,
            location = Locations.between(identifier, initializer)
        )

        return VariableStatement(
            declaration = declaration,
            location = Locations.between(letToken, semicolon)
        )
    }
}
