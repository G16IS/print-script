package printscript

import printscript.evaluator.RuleEvaluator
import printscript.evaluator.RuleHandlers
import printscript.domain.Grammar

object DefaultParserFactory {
    fun create(grammar: Grammar): Parser =
        DefaultParser(grammar, RuleEvaluator(grammar, RuleHandlers.defaults()))
}
