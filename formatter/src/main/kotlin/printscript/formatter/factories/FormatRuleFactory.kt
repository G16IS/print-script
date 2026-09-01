package printscript.formatter.factories

import printscript.error.FormatError
import printscript.formatter.rules.FormatRule
import printscript.util.Result

interface FormatRuleFactory {
    val types: Set<String>

    fun create(spec: ResolvedFormatRule): Result<FormatRule, FormatError>
}

object FormatRuleFactories {
    fun defaults(): List<FormatRuleFactory> =
        listOf(
            SpaceRuleFactory,
            NewlineRuleFactory,
        )
}
