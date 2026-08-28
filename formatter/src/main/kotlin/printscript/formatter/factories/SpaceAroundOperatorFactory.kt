package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.SpaceAroundOperatorRule
import printscript.util.Result

object SpaceAroundOperatorFactory : FormatRuleFactory {
    override val type: String = SpaceAroundOperatorRule.TYPE
    override val userConfigurable: Boolean = false

    override fun create(params: Map<String, Any?>): Result<FormatRule, FormatError> = Result.Ok(SpaceAroundOperatorRule)
}
