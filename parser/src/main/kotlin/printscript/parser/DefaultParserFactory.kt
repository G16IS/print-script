package printscript.parser

import printscript.common.ast.Location
import printscript.common.ast.Program
import printscript.Lexer
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
fun parse(tokens: Lexer): Program = DefaultParserFactory.create().parseNextStatement(tokens, Program(emptyList(),
    Location.empty()))
