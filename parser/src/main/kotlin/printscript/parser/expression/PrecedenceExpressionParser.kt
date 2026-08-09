package printscript.parser.expression

import printscript.common.ast.BinaryExpression
import printscript.common.ast.Expression
import printscript.common.ast.Identifier
import printscript.common.ast.NumberLiteral
import printscript.common.ast.StringLiteral
import printscript.lexer.Identifier as IdentifierToken
import printscript.lexer.LeftParen
import printscript.lexer.NumberLiteral as NumberLiteralToken
import printscript.lexer.Operator
import printscript.lexer.RightParen
import printscript.lexer.StringLiteral as StringLiteralToken
import printscript.lexer.Token
import printscript.parser.error.ParseException
import printscript.parser.token.TokenSource
import printscript.parser.util.Locations

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

    private fun parseExpression(tokens: TokenSource): Expression {
        var left = parseTerm(tokens)
        while (isOperator(tokens.peek(), "+", "-")) {
            val opToken = tokens.advance()
            val right = parseTerm(tokens)
            left = BinaryExpression(
                left = left,
                right = right,
                operation = operatorValue(opToken),
                location = Locations.between(left, right)
            )
        }
        return left
    }

    private fun parseTerm(tokens: TokenSource): Expression {
        var left = parseFactor(tokens)
        while (isOperator(tokens.peek(), "*", "/")) {
            val opToken = tokens.advance()
            val right = parseFactor(tokens)
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
            is NumberLiteralToken -> {
                tokens.advance()
                val raw = token.value.orElseThrow {
                    ParseException("Number literal missing value", Locations.of(token))
                }
                NumberLiteral(raw.toDouble(), Locations.of(token))
            }
            is StringLiteralToken -> {
                tokens.advance()
                val raw = token.value.orElseThrow {
                    ParseException("String literal missing value", Locations.of(token))
                }
                StringLiteral(normalizeStringLiteral(raw), Locations.of(token))
            }
            is IdentifierToken -> {
                tokens.advance()
                val name = token.value.orElseThrow {
                    ParseException("Identifier missing value", Locations.of(token))
                }
                Identifier(name, Locations.of(token))
            }
            is LeftParen -> {
                tokens.advance()
                val expr = parseExpression(tokens)
                tokens.expect({ it is RightParen }, "Expected ')' after expression")
                expr
            }
            else -> throw ParseException(
                "Expected expression, found ${token.type::class.simpleName}",
                Locations.of(token)
            )
        }
    }

    private fun isOperator(token: Token, vararg ops: String): Boolean {
        if (token.type !is Operator) return false
        val value = token.value.orElse(null) ?: return false
        return value in ops
    }

    private fun operatorValue(token: Token): String =
        token.value.orElseThrow {
            ParseException("Operator missing value", Locations.of(token))
        }

    /**
     * Lexer may keep surrounding quotes; strip one matching pair if present.
     */
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
