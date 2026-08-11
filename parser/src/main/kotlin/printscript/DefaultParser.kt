package printscript

import printscript.ast.Program
import printscript.ast.Statement
import printscript.error.ParseException
import printscript.expression.ExpressionParser
import printscript.statement.StatementParser
import printscript.token.LexerTokenSource
import printscript.token.TokenSource
import printscript.util.Locations

/**
 * Recursive-descent program parser. Delegates each statement to registered
 * [printscript.statement.StatementParser] strategies and expressions to [ExpressionParser].
 */
class DefaultParser(
    private val statementParsers: List<StatementParser>,
    private val expressionParser: ExpressionParser
) : Parser {
    override fun parseNextStatement(tokenStream: Lexer, program: Program): Program {
        val source = LexerTokenSource(tokenStream)

        val statement: Statement = parseStatement(source)
        val location = Locations.between(program.location, statement.location)

        return program.withStatement(statement).copy(location = location)
    }

    private fun parseStatement(source: TokenSource): Statement {
        for (parser in statementParsers) {
            val statement = parser
                .parse(source, expressionParser) ?: continue

            return statement
        }

        val token = source.peek()
        throw ParseException(
            "Unexpected token ${token.type.name}; expected start of statement",
            Locations.of(token)
        )
    }
}
