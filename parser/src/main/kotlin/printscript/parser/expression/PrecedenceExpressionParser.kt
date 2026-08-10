package printscript.parser.expression

import printscript.common.ast.BinaryExpression
import printscript.common.ast.Expression
import printscript.common.ast.Identifier
import printscript.common.ast.NumberLiteral
import printscript.common.ast.StringLiteral
import printscript.common.domain.Identifier as IdentifierToken
import printscript.common.domain.LeftParen
import printscript.common.domain.NumberLiteral as NumberLiteralToken
import printscript.common.domain.Operator
import printscript.common.domain.RightParen
import printscript.common.domain.StringLiteral as StringLiteralToken
import printscript.common.domain.Token
import printscript.parser.error.ParseErrors
import printscript.parser.error.ParseException
import printscript.parser.token.TokenSource
import printscript.parser.util.Locations
import printscript.parser.util.requireValue

/**
 * Classic recursive-descent expression parser:
 *  - expression := term (("+" | "-") term)*
 *  - term       := factor (("*" | "/") factor)*
 *  - factor     := NUMBER | STRING | IDENTIFIER | "(" expression ")"
 *
 * No semantic checks — only structure.
 */
class PrecedenceExpressionParser : ExpressionParser {

    override fun parse(tokens: TokenSource): Expression = parseExpression(tokens)

    private fun parseExpression(tokens: TokenSource): Expression =
        parseBinary(tokens, setOf("+", "-"), ::parseTerm)

    private fun parseTerm(tokens: TokenSource): Expression =
        parseBinary(tokens, setOf("*", "/"), ::parseFactor)

    private fun parseBinary(
        tokens: TokenSource,
        operators: Set<String>,
        next: (TokenSource) -> Expression
    ): Expression {
        var left = next(tokens)

        while (isOperator(tokens.peek(), operators)) {
            val opToken = tokens.advance()
            val right = next(tokens)
            left = BinaryExpression(
                left = left,
                right = right,
                operation = operatorValue(opToken),
                location = Locations.between(left, right)
            )
        }

        return left
    }

    private fun parseFactor(tokens: TokenSource): Expression {
        val token = tokens.peek()

        return when (token.type) {
            is NumberLiteralToken -> parseNumberLiteral(tokens, token)
            is StringLiteralToken -> parseStringLiteral(tokens, token)
            is IdentifierToken -> parseIdentifier(tokens, token)
            is LeftParen -> parseGroupedExpression(tokens)

            else -> throw ParseErrors.unexpectedToken(token, "expression")
        }
    }

    private fun parseNumberLiteral(tokens: TokenSource, token: Token): NumberLiteral {
        tokens.advance()
        return NumberLiteral(
            value = token.requireValue("Number literal").toDouble(),
            location = Locations.of(token)
        )
    }

    private fun parseStringLiteral(tokens: TokenSource, token: Token): StringLiteral {
        tokens.advance()
        return StringLiteral(
            value = normalizeStringLiteral(token.requireValue("String literal")),
            location = Locations.of(token)
        )
    }

    private fun parseIdentifier(tokens: TokenSource, token: Token): Identifier {
        tokens.advance()
        return Identifier(
            name = token.requireValue("Identifier"),
            location = Locations.of(token)
        )
    }

    private fun parseGroupedExpression(tokens: TokenSource): Expression {
        tokens.advance() // consume '('
        val expr = parseExpression(tokens)
        tokens.expect({ it is RightParen }, "Expected ')' after expression")
        return expr
    }

    // Helpers

    private fun isOperator(token: Token, operators: Set<String>): Boolean {
        if (token.type !is Operator) return false
        val value = token.value.orElse(null) ?: return false
        return value in operators
    }

    private fun operatorValue(token: Token): String =
        token.value.orElseThrow {
            ParseException("Operator missing value", Locations.of(token))
        }

    private fun normalizeStringLiteral(raw: String): String {
        if (raw.length >= 2) {
            val first = raw.first()
            val last = raw.last()
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return raw.substring(1, raw.length - 1)
            }
        }
        return raw
    }
}