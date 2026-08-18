package printscript

import printscript.parse.RuleEvaluator
import printscript.parse.RuleHandlers
import printscript.domain.Grammar

object DefaultParserFactory {
    fun create(grammar: Grammar): Parser =
        DefaultParser(grammar, RuleEvaluator(grammar, RuleHandlers.defaults()))
}
