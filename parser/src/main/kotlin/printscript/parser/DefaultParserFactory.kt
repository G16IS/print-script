package printscript.parser

import printscript.domain.Grammar
import printscript.parser.parse.RuleEvaluator
import printscript.parser.parse.RuleHandler

internal object DefaultParserFactory {
    fun create(
        grammar: Grammar,
        handlers: List<RuleHandler>,
    ): Parser = DefaultParser(grammar, RuleEvaluator(grammar, handlers))
}
