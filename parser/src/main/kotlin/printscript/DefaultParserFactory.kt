package printscript

import printscript.ast.Location
import printscript.ast.Program
import printscript.expression.PrecedenceExpressionParser
import printscript.statement.PrintStatementParser
import printscript.statement.VariableStatementParser

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