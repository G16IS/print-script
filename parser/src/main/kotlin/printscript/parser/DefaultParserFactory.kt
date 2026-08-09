package printscript.parser

import printscript.lexer.Token
import printscript.common.ast.Program
import printscript.parser.expression.PrecedenceExpressionParser
import printscript.parser.statement.PrintStatementParser
import printscript.parser.statement.VariableStatementParser

/**
 * Wires the default PrintScript v1 statement strategies and expression parser.
 */
object DefaultParserFactory {
    fun create(): Parser = DefaultParser(
        statementParsers = listOf(
            VariableStatementParser(),
            PrintStatementParser()
        ),
        expressionParser = PrecedenceExpressionParser()
    )
}

/** Convenience entry point using [DefaultParserFactory]. */
fun parse(tokens: List<Token>): Program = DefaultParserFactory.create().parse(tokens)
