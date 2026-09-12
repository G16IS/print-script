package printscript

import printscript.domain.Grammar
import printscript.parse.RuleEvaluator
import printscript.parse.RuleHandler

object DefaultParserFactory {
    fun create(
        grammar: Grammar,
        handlers: List<RuleHandler>,
    ): Parser = DefaultParser(grammar, RuleEvaluator(grammar, handlers))
}
