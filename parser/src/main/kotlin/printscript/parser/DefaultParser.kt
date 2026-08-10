package printscript.parser

import printscript.common.ast.Location
import printscript.common.ast.Program
import printscript.common.ast.Statement
import printscript.lexer.Lexer
import printscript.common.domain.Token
import printscript.parser.error.ParseException
import printscript.parser.expression.ExpressionParser
import printscript.parser.statement.StatementParser
import printscript.parser.token.LexerTokenSource
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
            "Unexpected token ${token.type::class.simpleName}; expected start of statement",
            Locations.of(token)
        )
    }
}
