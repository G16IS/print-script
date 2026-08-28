package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.rules.FormatRule
import printscript.util.Result

interface FormatRuleFactory {
    val type: String
    val userConfigurable: Boolean

    fun create(params: Map<String, Any?>): Result<FormatRule, FormatError>
}

object FormatRuleFactories {
    fun defaults(): List<FormatRuleFactory> =
        listOf(
            SpaceAroundOperatorFactory,
            NewlineAfterSemicolonFactory,
            MaxOneSpaceFactory,
            SpaceAfterLetFactory,
            SpaceBeforeColonFactory,
            SpaceAfterColonFactory,
            SpaceAroundAssignFactory,
            NewlinesBeforePrintlnFactory,
        )
}
