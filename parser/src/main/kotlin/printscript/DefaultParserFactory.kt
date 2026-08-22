package printscript

import printscript.domain.Grammar
import printscript.parse.RuleEvaluator
import printscript.parse.RuleHandlers

object DefaultParserFactory {
    fun create(grammar: Grammar): Parser = DefaultParser(grammar, RuleEvaluator(grammar, RuleHandlers.defaults()))
}
