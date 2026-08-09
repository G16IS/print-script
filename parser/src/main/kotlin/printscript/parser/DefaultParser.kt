package printscript.parser

import printscript.common.ast.Program
import printscript.common.ast.Statement
import printscript.lexer.Token
import printscript.parser.error.ParseException
import printscript.parser.expression.ExpressionParser
import printscript.parser.statement.StatementParser
import printscript.parser.token.ListTokenSource
import printscript.parser.token.TokenSource
import printscript.parser.util.Locations

/**
 * Recursive-descent program parser. Delegates each statement to registered
 * [StatementParser] strategies and expressions to [ExpressionParser].
 */
class DefaultParser(
    private val statementParsers: List<StatementParser>,
    private val expressionParser: ExpressionParser
) : Parser {
    override fun parse(tokens: List<Token>): Program {
        val source = ListTokenSource(tokens)
        val statements = mutableListOf<Statement>()

        while (!source.isAtEnd()) {
            statements += parseStatement(source)
        }

        val location = if (statements.isEmpty()) {
            Locations.empty()
        } else {
            Locations.between(statements.first(), statements.last())
        }

        return Program(statements.toList(), location)
    }

    private fun parseStatement(source: TokenSource): Statement {
        for (parser in statementParsers) {
            val statement = parser.parse(source, expressionParser)
            if (statement != null) return statement
        }
        val token = source.peek()
        throw ParseException(
            "Unexpected token ${token.type::class.simpleName}; expected start of statement",
            Locations.of(token)
        )
    }
}
