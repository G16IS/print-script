package printscript.application.factory.parser

import printscript.DefaultParserFactory
import printscript.Parser
import printscript.domain.Grammar
import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
import printscript.parse.AtomRuleHandler
import printscript.parse.LeftRuleHandler
import printscript.parse.OrRuleHandler
import printscript.parse.RepeatRuleHandler
import printscript.parse.RuleHandler
import printscript.parse.SeqRuleHandler
import printscript.util.Result
import printscript.util.flatMap

object ParserFactory {
    fun create(
        grammar: Grammar,
        version: String,
    ): Result<Parser, RuntimeError> =
        getRuleHandlers(version).flatMap { handlers ->
            Result.Ok(DefaultParserFactory.create(grammar, handlers))
        }

    private fun getRuleHandlers(version: String): Result<List<RuleHandler>, RuntimeError> =
        when (version) {
            "1" -> Result.Ok(handlersV1())
            else -> Result.Err(LanguageVersionNotFound(version))
        }

    private fun handlersV1(): List<RuleHandler> =
        listOf(
            AtomRuleHandler(),
            SeqRuleHandler(),
            OrRuleHandler(),
            LeftRuleHandler(),
            RepeatRuleHandler(),
        )
}
